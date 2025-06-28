package com.echo.virtual_interview.utils.rtasr;

import com.echo.virtual_interview.constant.IflytekProperties;
import com.echo.virtual_interview.iflytek.ws.DraftWithOrigin;
import com.echo.virtual_interview.model.dto.interview.ChatMessage;
import com.echo.virtual_interview.service.IInterviewService;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.drafts.Draft;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * 负责处理ASR（自动语音识别）的业务流程。
 * 核心功能：
 * 1. 管理与讯飞ASR服务器的WebSocket连接生命周期。
 * 2. 【核心优化】为每个会话创建一个带缓冲的、有节流阀的发送机制，避免因发送过快导致的服务端错误。
 * 3. 缓存并拼接实时的识别结果。
 * 4. 在语音流结束后，将完整的识别文本送入下一级业务（如AI服务）。
 * 5. 【新增调试功能】将接收到的音频数据存储到本地文件，以便验证。
 */
@Slf4j
@Service
public class AsrProcessingService {

    private static final byte[] END_OF_STREAM_MARKER = new byte[0];
    // --- 【新增】定义音频日志存储目录 ---
    private static final Path AUDIO_LOG_DIRECTORY = Paths.get("asr_audio_logs");

    private final IInterviewService interviewService;
    private final SimpMessagingTemplate messagingTemplate;
    private final IflytekProperties iflytekProperties;
    private final Map<String, IflytekAsrClient> activeClients = new ConcurrentHashMap<>();
    private final Map<String, StringBuilder> transcriptBuffers = new ConcurrentHashMap<>();
    private final Map<String, BlockingQueue<byte[]>> audioQueues = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> sendingTasks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService sendingScheduler;

    public AsrProcessingService(IflytekProperties iflytekProperties, IInterviewService interviewService, SimpMessagingTemplate messagingTemplate) {
        this.iflytekProperties = iflytekProperties;
        this.interviewService = interviewService;
        this.messagingTemplate = messagingTemplate;
        int corePoolSize = Math.max(4, Runtime.getRuntime().availableProcessors());
        this.sendingScheduler = Executors.newScheduledThreadPool(corePoolSize);
        log.info("ASR发送调度器已初始，核心线程数: {}", corePoolSize);

/*        // --- 【新增】在服务启动时创建日志目录 ---
        try {
            if (!Files.exists(AUDIO_LOG_DIRECTORY)) {
                Files.createDirectories(AUDIO_LOG_DIRECTORY);
                log.info("成功创建音频日志目录: {}", AUDIO_LOG_DIRECTORY.toAbsolutePath());
            }
        } catch (IOException e) {
            log.error("创建音频日志目录失败！", e);
        }*/
    }

    /**
     * 【新增存储逻辑】处理前端发来的音频数据块。
     * 将数据存入文件，然后放入队列。
     */
    public void processAudioChunk(String sessionId, Integer userId, byte[] audioData) {
//        // 【新增】在放入队列前，先将音频数据写入文件
//        saveAudioToFile(sessionId, audioData);

        activeClients.computeIfAbsent(sessionId, key -> createAndConnectClient(key, userId));

        BlockingQueue<byte[]> queue = audioQueues.get(sessionId);
        if (queue != null) {
            boolean offered = queue.offer(audioData);
            if (!offered) {
                log.warn("会话 {} 的音频缓冲队列已满，可能会丢失数据。", sessionId);
            }
        } else {
            log.error("严重错误: 会话 {} 对应的音频队列未找到！", sessionId);
        }
    }

    public void endAudioStream(String sessionId) {
        log.info("会话 {} 收到结束信号，将结束标记放入队列。", sessionId);
        BlockingQueue<byte[]> queue = audioQueues.get(sessionId);
        if (queue != null) {
            queue.offer(END_OF_STREAM_MARKER);
        } else {
            log.warn("会话 {} 的音频队列已不存在，可能已被清理。", sessionId);
        }
    }

    private IflytekAsrClient createAndConnectClient(String sessionId, Integer userId) {
        log.info("为会话 {} 创建新的讯飞ASR客户端及相关资源", sessionId);

        transcriptBuffers.put(sessionId, new StringBuilder());
        audioQueues.put(sessionId, new LinkedBlockingQueue<>());

        try {
            String url = IflytekAsrClient.getHandshakeUrl(
                    iflytekProperties.getHost(),
                    iflytekProperties.getAppid(),
                    iflytekProperties.getSecretKey()
            );
            Draft draft = new DraftWithOrigin(iflytekProperties.getHost());

            Consumer<String> onMessageCallback = (transcribedText) -> {
                StringBuilder buffer = transcriptBuffers.get(sessionId);
                if (StringUtils.hasText(transcribedText) && buffer != null) {
                    log.info("[实时转写] 会话: {} 追加文本: '{}'", sessionId, transcribedText);
                    buffer.append(transcribedText);
                }
            };

            Runnable onCloseCallback = () -> {
                log.info("讯飞连接关闭，开始处理会话 {} 的完整转写结果并清理资源。", sessionId);
                StringBuilder finalTranscriptBuilder = transcriptBuffers.get(sessionId);
                String fullText = (finalTranscriptBuilder != null) ? finalTranscriptBuilder.toString().trim() : "";

                if (StringUtils.hasText(fullText)) {
                    log.info("[业务流] 会话: {}, 用户: {}. 完整识别文本: '{}'，即将发送给AI处理。", sessionId, userId, fullText);
                    interviewService.interviewProcess(fullText, sessionId, userId)
                            .subscribe(aiReply -> {
                                log.info("发送AI回复给用户 {}: {}", userId, aiReply);
                                messagingTemplate.convertAndSendToUser(
                                        userId.toString(),
                                        "/queue/interview/answer",
                                        new ChatMessage("AI", aiReply)
                                );
                            });
                } else {
                    log.warn("[业务流] 会话: {} 的最终转写文本为空，不调用AI服务。", sessionId);
                }
                cleanupSessionResources(sessionId);
            };

            IflytekAsrClient client = new IflytekAsrClient(new URI(url), draft, onMessageCallback, onCloseCallback);
            client.setConnectionLostTimeout(60);
            client.connectBlocking(5, TimeUnit.SECONDS);

            if (client.isOpen()) {
                log.info("讯飞ASR客户端连接成功，启动定时发送任务。SessionID: {}", sessionId);
                ScheduledFuture<?> task = sendingScheduler.scheduleAtFixedRate(() -> {
                    try {
                        BlockingQueue<byte[]> queue = audioQueues.get(sessionId);
                        IflytekAsrClient currentClient = activeClients.get(sessionId);

                        if (currentClient == null || !currentClient.isOpen() || queue == null) {
                            return;
                        }

                        byte[] chunk = queue.take();

                        if (chunk == END_OF_STREAM_MARKER) {
                            log.info("检测到会话 {} 的结束标记，正在发送最后的数据帧并停止任务。", sessionId);
                            currentClient.sendEnd();
                            ScheduledFuture<?> self = sendingTasks.get(sessionId);
                            if (self != null) {
                                self.cancel(false);
                            }
                        } else {
                            currentClient.sendAudio(chunk);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.warn("会话 {} 的音频发送任务被中断。", sessionId);
                    } catch (Exception e) {
                        log.error("会话 {} 的音频发送任务出现未知异常，将停止该任务。", sessionId, e);
                        ScheduledFuture<?> self = sendingTasks.get(sessionId);
                        if (self != null) {
                            self.cancel(true);
                        }
                    }
                }, 40, 40, TimeUnit.MILLISECONDS);

                sendingTasks.put(sessionId, task);
                return client;
            } else {
                log.error("讯飞ASR客户端连接失败. SessionID: {}", sessionId);
                cleanupSessionResources(sessionId);
                return null;
            }

        } catch (Exception e) {
            log.error("创建讯飞ASR客户端时发生严重异常. SessionID: {}", sessionId, e);
            cleanupSessionResources(sessionId);
            return null;
        }
    }

    /**
     * 【新增】将音频数据块追加到本地文件。
     * @param sessionId 用于文件名的会话ID。
     * @param audioData 要写入的音频数据。
     */
    private void saveAudioToFile(String sessionId, byte[] audioData) {
        // 使用sessionId作为文件名，确保每个会话的音频保存在独立文件中
        Path audioFile = AUDIO_LOG_DIRECTORY.resolve(sessionId + ".pcm");
        try (FileOutputStream fos = new FileOutputStream(audioFile.toFile(), true)) {
            fos.write(audioData);
        } catch (IOException e) {
            log.error("将会话 {} 的音频数据写入文件 {} 失败。", sessionId, audioFile.toAbsolutePath(), e);
        }
    }

    private void cleanupSessionResources(String sessionId) {
        log.info("正在清理会话 {} 的所有资源...", sessionId);

        ScheduledFuture<?> task = sendingTasks.remove(sessionId);
        if (task != null) {
            task.cancel(true);
        }

        activeClients.remove(sessionId);

        BlockingQueue<byte[]> queue = audioQueues.remove(sessionId);
        if (queue != null) {
            queue.clear();
        }

        transcriptBuffers.remove(sessionId);

        log.info("会话 {} 的资源清理完毕。", sessionId);
//        log.info("会话 {} 的完整音频已保存至: {}", sessionId, AUDIO_LOG_DIRECTORY.resolve(sessionId + ".pcm").toAbsolutePath());
    }

    @PreDestroy
    public void shutdown() {
        log.info("正在关闭AsrProcessingService，清理所有活动连接和任务...");
        activeClients.values().forEach(IflytekAsrClient::close);
        sendingScheduler.shutdownNow();
        try {
            if (!sendingScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                log.error("ASR发送调度器未能按时关闭。");
            }
        } catch (InterruptedException e) {
            sendingScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        activeClients.clear();
        transcriptBuffers.clear();
        audioQueues.clear();
        sendingTasks.clear();
        log.info("AsrProcessingService已成功关闭。");
    }
}
