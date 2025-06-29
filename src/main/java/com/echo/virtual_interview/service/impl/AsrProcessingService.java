package com.echo.virtual_interview.service.impl;

import com.echo.virtual_interview.constant.IflytekProperties;
import com.echo.virtual_interview.iflytek.ws.DraftWithOrigin;
import com.echo.virtual_interview.model.dto.interview.ChatMessage;
import com.echo.virtual_interview.model.dto.interview.audio.AudioResponseDto;
import com.echo.virtual_interview.service.IInterviewService;
import com.echo.virtual_interview.utils.rtasr.IflytekAsrClient;
import com.echo.virtual_interview.utils.tst.TtsService;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.drafts.Draft;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
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
    // --- 【新增】定义用于切分句子的标点符号集 ---
    private static final String SENTENCE_DELIMITERS = "，。！？,!?";


    private final IInterviewService interviewService;
    private final SimpMessagingTemplate messagingTemplate;
    private final IflytekProperties iflytekProperties;
    private final Map<String, IflytekAsrClient> activeClients = new ConcurrentHashMap<>();
    private final Map<String, StringBuilder> transcriptBuffers = new ConcurrentHashMap<>();
    private final Map<String, BlockingQueue<byte[]>> audioQueues = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> sendingTasks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService sendingScheduler;
    @Autowired
    private TtsService ttsService; // 注入语音合成服务

    public AsrProcessingService(IflytekProperties iflytekProperties, IInterviewService interviewService, SimpMessagingTemplate messagingTemplate) {
        this.iflytekProperties = iflytekProperties;
        this.interviewService = interviewService;
        this.messagingTemplate = messagingTemplate;
        int corePoolSize = Math.max(4, Runtime.getRuntime().availableProcessors());
        this.sendingScheduler = Executors.newScheduledThreadPool(corePoolSize);
        log.info("ASR发送调度器已初始，核心线程数: {}", corePoolSize);

/* // --- 【新增】在服务启动时创建日志目录 ---
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
    /**
     * 【新增存储逻辑】处理前端发来的音频数据块。
     * 将数据存入文件，然后放入队列。
     */
    public void processAudioChunkTTS(String sessionId, Integer userId, byte[] audioData) {
//        // 【新增】在放入队列前，先将音频数据写入文件
//        saveAudioToFile(sessionId, audioData);

        activeClients.computeIfAbsent(sessionId, key -> createAndConnectClientTTS(key, userId));

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

    // 语音-文本，返回流式文本
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

    // 语音-文本-语音 返回流式语音
    private IflytekAsrClient createAndConnectClientTTS(String sessionId, Integer userId) {
        log.info("为会话 {} 创建新的讯飞ASR客户端及相关资源 (TTS流式响应模式)", sessionId);

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

            // 【MODIFIED】当用户语音输入结束后的回调
            Runnable onCloseCallback = () -> {
                log.info("讯飞连接关闭，开始处理会话 {} 的完整转写结果并清理资源。", sessionId);
                StringBuilder finalTranscriptBuilder = transcriptBuffers.get(sessionId);
                String fullText = (finalTranscriptBuilder != null) ? finalTranscriptBuilder.toString().trim() : "";

                if (StringUtils.hasText(fullText)) {
                    // 将AI回复处理委托给新的流式处理方法
                    handleStreamingAiResponse(fullText, sessionId, userId);
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
     * 【NEW】处理来自AI服务的流式文本响应，并分段进行TTS。
     * @param fullText 用户说的话，用于发送给AI服务。
     * @param sessionId 当前会话ID。
     * @param userId    当前用户ID。
     */
    private void handleStreamingAiResponse(String fullText, String sessionId, Integer userId) {
        log.info("[业务流] 会话: {}, 用户: {}. 完整识别文本: '{}'，即将流式发送给AI处理。", sessionId, userId, fullText);

        final StringBuilder aiResponseBuffer = new StringBuilder();
        // 使用数组以便在lambda表达式中修改其值
        final int[] lastTtsPosition = {0};

        // 假设 interviewService.interviewProcess 返回一个支持流式处理的响应 (e.g., Project Reactor's Flux)
        interviewService.interviewProcess(fullText, sessionId, userId)
                .doOnNext(aiReplyChunk -> {
                    aiResponseBuffer.append(aiReplyChunk);
                    log.trace("AI回复流: 收到数据块 -> '{}'", aiReplyChunk);

                    String currentText = aiResponseBuffer.toString();
                    int searchStart = lastTtsPosition[0];
                    int sentenceEnd;

                    // 循环处理缓冲区中所有完整的句子
                    while ((sentenceEnd = findNextSentenceEnd(currentText, searchStart)) != -1) {
                        String sentenceToSpeak = currentText.substring(searchStart, sentenceEnd + 1);
                        log.info("提取到待合成句子: '{}'，发送给TTS。", sentenceToSpeak);

                        Consumer<String> onAudioReceived = audioBase64 -> sendAudioChunkToUser(userId, audioBase64);
                        Runnable onSentenceComplete = () -> log.info("句子 '{}' 合成完毕。", sentenceToSpeak);
                        Consumer<String> onSentenceFailed = errorMsg -> log.error("句子 '{}' 合成失败: {}", sentenceToSpeak, errorMsg);

                        ttsService.synthesize(sentenceToSpeak, onAudioReceived, onSentenceComplete, onSentenceFailed);

                        // 更新下一次搜索的起始位置
                        searchStart = sentenceEnd + 1;
                    }
                    // 更新全局已处理文本的位置
                    lastTtsPosition[0] = searchStart;
                })
                .doOnComplete(() -> {
                    // AI文本流结束，处理可能遗留在缓冲区中的最后一部分文本
                    String remainingText = aiResponseBuffer.substring(lastTtsPosition[0]);
                    if (StringUtils.hasText(remainingText)) {
                        log.info("处理流结束后剩余的文本: '{}'，发送给TTS。", remainingText);

                        Consumer<String> onAudioReceived = audioBase64 -> sendAudioChunkToUser(userId, audioBase64);
                        // 这是最后一次合成，完成后需要向前端发送结束信号
                        Runnable onFinalSynthesisComplete = () -> sendFinalAudioSignalToUser(userId);
                        Consumer<String> onSynthesisFailed = errorMsg -> {
                            log.error("剩余文本 '{}' 合成失败: {}", remainingText, errorMsg);
                            // 失败时也要发送结束信号，以防前端一直等待
                            sendFinalAudioSignalToUser(userId);
                        };

                        ttsService.synthesize(remainingText, onAudioReceived, onFinalSynthesisComplete, onSynthesisFailed);
                    } else {
                        // 如果没有剩余文本，直接发送结束信号
                        log.info("用户 {} 的AI回复语音流处理完毕，无剩余文本。", userId);
                        sendFinalAudioSignalToUser(userId);
                    }
                })
                .doOnError(error -> {
                    log.error("处理AI回复流时发生严重错误. SessionID: {}", sessionId, error);
                    // 出现错误时，务必发送结束信号，避免客户端卡死
                    sendFinalAudioSignalToUser(userId);
                })
                .subscribe(); // 启动响应流的消费
    }

    /**
     * 【NEW】从指定位置开始，查找文本中下一个句末标点符号的位置。
     * @param text 要搜索的文本。
     * @param startIndex 搜索的起始索引。
     * @return 标点符号的索引，如果未找到则返回-1。
     */
    private int findNextSentenceEnd(String text, int startIndex) {
        for (int i = startIndex; i < text.length(); i++) {
            if (SENTENCE_DELIMITERS.indexOf(text.charAt(i)) != -1) {
                return i;
            }
        }
        return -1; // 未找到
    }

    /**
     * 【NEW】将音频数据块通过STOMP发送给指定用户。
     */
    private void sendAudioChunkToUser(Integer userId, String audioBase64) {
        log.trace("为用户 {} 发送一个音频数据块", userId);
        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/interview/audio.reply",
                new AudioResponseDto(audioBase64, false)
        );
    }

    /**
     * 【NEW】向指定用户发送音频流结束的信号。
     */
    private void sendFinalAudioSignalToUser(Integer userId) {
        log.info("用户 {} 的AI回复语音合成完成，发送结束信号。", userId);
        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/interview/audio.reply",
                new AudioResponseDto(null, true)
        );
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