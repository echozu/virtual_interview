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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * [已修改] 负责处理ASR（自动语音识别）和TTS（语音合成）的完整业务流程。
 * 核心功能：
 * 1. 管理与讯飞ASR服务器的WebSocket连接生命周期。
 * 2. 在语音流结束后，将完整的识别文本送入下一级业务（如AI服务）。
 * 3. 【新增】处理来自AI服务的流式文本，将其切分为句子，并实时进行TTS合成。
 * 4. 【新增】将TTS合成的音频流实时转发给前端。
 */
@Slf4j
@Service
public class AsrProcessingService {

    private static final byte[] END_OF_STREAM_MARKER = new byte[0];
    private static final Path AUDIO_LOG_DIRECTORY = Paths.get("asr_audio_logs");
    // 定义用于切分句子的标点符号集
    private static final String SENTENCE_DELIMITERS = "，。！？,!?";

    private final IInterviewService interviewService;
    private final SimpMessagingTemplate messagingTemplate;
    private final IflytekProperties iflytekProperties;
    private final Map<String, IflytekAsrClient> activeClients = new ConcurrentHashMap<>();
    private final Map<String, StringBuilder> transcriptBuffers = new ConcurrentHashMap<>();
    private final Map<String, BlockingQueue<byte[]>> audioQueues = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> sendingTasks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService sendingScheduler;
    // 【新增】定义用户实时转写文本的目标地址
    private static final String DEST_USER_TRANSCRIPT = "/queue/interview/transcript.user";

    // 【新增】定义AI实时回复文本的目标地址
    private static final String DEST_AI_TRANSCRIPT = "/queue/interview/transcript.assistant";
    @Autowired
    private TtsService ttsService; // 注入语音合成服务

    public AsrProcessingService(IflytekProperties iflytekProperties, IInterviewService interviewService, SimpMessagingTemplate messagingTemplate) {
        this.iflytekProperties = iflytekProperties;
        this.interviewService = interviewService;
        this.messagingTemplate = messagingTemplate;
        int corePoolSize = Math.max(4, Runtime.getRuntime().availableProcessors());
        this.sendingScheduler = Executors.newScheduledThreadPool(corePoolSize);
        log.info("ASR发送调度器已初始，核心线程数: {}", corePoolSize);
    }

    // ... processAudioChunk, endAudioStream, createAndConnectClient 等方法保持不变 ...
    public void processAudioChunk(String sessionId, Integer userId, byte[] audioData) {
        activeClients.computeIfAbsent(sessionId, key -> createAndConnectClient(key, userId));
        BlockingQueue<byte[]> queue = audioQueues.get(sessionId);
        if (queue != null) {
            if (!queue.offer(audioData)) {
                log.warn("会话 {} 的音频缓冲队列已满，可能会丢失数据。", sessionId);
            }
        } else {
            log.error("严重错误: 会话 {} 对应的音频队列未找到！", sessionId);
        }
    }

    public void processAudioChunkTTS(String sessionId, Integer userId, byte[] audioData) {
        activeClients.computeIfAbsent(sessionId, key -> createAndConnectClientTTS(key, userId));
        BlockingQueue<byte[]> queue = audioQueues.get(sessionId);
        if (queue != null) {
            if (!queue.offer(audioData)) {
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
                                        new ChatMessage("assistant", aiReply)
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

            // 1. ASR实时转写回调：将识别的文字片段追加到缓冲区
            // --- 修改这里的 onMessageCallback ---
            Consumer<String> onMessageCallback = (transcribedText) -> {
                // 1. 原有逻辑：将结果追加到内部缓冲区，用于最后生成完整文本
                StringBuilder buffer = transcriptBuffers.get(sessionId);
                if (StringUtils.hasText(transcribedText) && buffer != null) {
                    log.info("[实时转写] 会话: {} 追加文本: '{}'", sessionId, transcribedText);
                    buffer.append(transcribedText);
                }

                // 2. 【新增逻辑】将这个实时的、不完整的用户转写文本块，发送给前端
                if (StringUtils.hasText(transcribedText)) {
                    messagingTemplate.convertAndSendToUser(
                            userId.toString(),
                            DEST_USER_TRANSCRIPT,
                            new ChatMessage("user", transcribedText) // 使用您已有的ChatMessage DTO
                    );
                }
            };

            // 2. ASR结束回调：当用户一句话说完，ASR连接关闭时触发
            Runnable onCloseCallback = () -> {
                log.info("ASR连接关闭，开始处理会话 {} 的完整转写结果。", sessionId);
                StringBuilder finalTranscriptBuilder = transcriptBuffers.get(sessionId);
                String fullText = (finalTranscriptBuilder != null) ? finalTranscriptBuilder.toString().trim() : "";

                if (StringUtils.hasText(fullText)) {
                    // 【关键修复】调用重构后的、完全响应式的处理方法
                    handleStreamingAiResponse_Reactive(fullText, sessionId, userId);
                } else {
                    log.warn("[业务流] 会话: {} 的最终转写文本为空，不调用AI服务。", sessionId);
                    // 即使不调用AI，也要清理ASR相关的资源
                    cleanupSessionResources(sessionId);
                }
            };
            // 创建并连接ASR客户端...
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
     * 处理来自AI服务的流式文本响应，并分段进行TTS。
     */
    private void handleStreamingAiResponse(String fullText, String sessionId, Integer userId) {
        log.info("[业务流] 会话: {}, 用户: {}. 完整识别文本: '{}' -> 即将流式请求AI并转为语音", sessionId, userId, fullText);

        // 为本次AI回复创建一个唯一的句子处理器
        // 传入一个原子计数器来追踪待完成的TTS任务数
        SentenceProcessor processor = new SentenceProcessor(userId, new AtomicInteger(0));

        // 调用AI服务，并订阅其返回的流式文本
        interviewService.interviewProcess(fullText, sessionId, userId)
                .doOnNext(aiReplyChunk -> {
                    // 1.将AI返回的文本块，实时发送给前端
                    if (StringUtils.hasText(aiReplyChunk)) {
                        messagingTemplate.convertAndSendToUser(
                                userId.toString(),
                                DEST_AI_TRANSCRIPT,
                                new ChatMessage("assistant", aiReplyChunk)
                        );
                    }

                    // 2. 原有逻辑：将文本块交给句子处理器，用于后续的TTS
                    processor.processText(aiReplyChunk);
                })
                .doOnComplete(() -> {
                    log.info("AI文本流接收完毕，开始处理缓冲区中最后的内容。SessionID: {}", sessionId);
                    processor.setAiStreamFinished(true);

                })
                .doOnError(error -> {
                    log.error("处理AI回复流时发生错误，将向前端发送结束信号。SessionID: {}", sessionId, error);
                    sendFinalAudioSignalToUser(userId);
                })
                .doFinally(signalType -> {
                    cleanupSessionResources(sessionId);
                    log.info("AI->TTS流程结束，已清理ASR资源。SessionID: {}", sessionId);
                })
                .subscribe();
    }

    /**
     * 将一个完整的句子发送到TTS服务进行合成
     * @param sentence 要合成的句子
     * @param userId 目标用户ID
     * @param isFinalSentence 这是否是整个AI回复的最后一个句子
     */
    /**
     * 将一个完整的句子发送到TTS服务进行合成
     */
    private void sendCompleteSentenceToTTS(String sentence, Integer userId, Runnable onSynthesisComplete) {
        if (!StringUtils.hasText(sentence)) {
            // 如果句子为空，直接执行完成回调
            if (onSynthesisComplete != null) onSynthesisComplete.run();
            return;
        }
        log.info("发送句子到TTS: '{}'", sentence);

        Consumer<String> onAudioReceived = audioBase64 -> {
            AudioResponseDto audioDto = new AudioResponseDto(audioBase64, false);
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/interview/audio.reply",
                    audioDto
            );
        };

        // onComplete不再关心是否是最后一句，它只负责调用传入的回调
        Runnable onComplete = () -> {
            log.info("句子 '{}' 合成完毕。", sentence);
            if (onSynthesisComplete != null) onSynthesisComplete.run();
        };

        Consumer<String> onError = errorMsg -> {
            log.error("句子 '{}' 合成失败: {}", sentence, errorMsg);
            if (onSynthesisComplete != null) onSynthesisComplete.run(); // 失败也算完成，以防锁死计数器
        };

        try {
            ttsService.synthesize(sentence, onAudioReceived, onComplete, onError);
        } catch (Exception e) {
            log.error("调用TTS服务时发生异常", e);
            onError.accept(e.getMessage());
        }
    }

    /**
     * 向指定用户发送音频流结束的信号。
     */
    private void sendFinalAudioSignalToUser(Integer userId) {
        log.info("AI回复语音全部合成完毕，向用户 {} 发送结束信号。", userId);
        AudioResponseDto finalDto = new AudioResponseDto(null, true);
        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/interview/audio.reply",
                finalDto
        );
    }

    /**
     * 【全新】内部类，负责缓冲和解析句子。
     */
    // 在 AsrProcessingService.java 中

    /**
     * 【最终修复版】内部类，负责缓冲和解析句子。
     */
    private class SentenceProcessor {
        private final StringBuilder buffer = new StringBuilder();
        private final Integer userId;
        private final AtomicInteger pendingTtsTasks;
        private volatile boolean aiStreamFinished = false;

        public SentenceProcessor(Integer userId, AtomicInteger pendingTtsTasks) {
            this.userId = userId;
            this.pendingTtsTasks = pendingTtsTasks;
        }

        public void setAiStreamFinished(boolean finished) {
            this.aiStreamFinished = finished;
            log.debug("AI stream is marked as finished.");
        }

        public void processText(String textChunk) {
            buffer.append(textChunk);
            processBuffer();
        }

        public void flush() {
            // 当AI流结束后，处理缓冲区里可能剩余的最后一部分文本
            if (!buffer.isEmpty()) {
                String remainingText = buffer.toString();
                buffer.setLength(0); // 清空缓冲区

                // 将剩余文本作为最后一个任务发出
                pendingTtsTasks.incrementAndGet();
                log.debug("Flushing remaining text. Pending tasks incremented to: {}", pendingTtsTasks.get());
                sendCompleteSentenceToTTS(remainingText, this.userId, this::onTaskCompleted);
            } else {
                // 如果缓冲区是空的，说明所有句子都已在processBuffer中发出
                // 此时只需检查是否所有任务都已完成
                checkIfAllDone();
            }
        }

        private void processBuffer() {
            int sentenceEndIndex;
            while ((sentenceEndIndex = findNextSentenceEnd(buffer.toString())) != -1) {
                String sentence = buffer.substring(0, sentenceEndIndex + 1);
                buffer.delete(0, sentenceEndIndex + 1);

                // 每发起一个TTS任务，计数器加1
                pendingTtsTasks.incrementAndGet();
                log.debug("New sentence found. Pending tasks incremented to: {}", pendingTtsTasks.get());
                sendCompleteSentenceToTTS(sentence, this.userId, this::onTaskCompleted);
            }
        }

        /**
         * 当一个TTS任务（成功或失败）完成时，调用此方法。
         */
        private void onTaskCompleted() {
            // 一个任务完成，计数器减1
            int remainingTasks = pendingTtsTasks.decrementAndGet();
            log.debug("A TTS task completed. Pending tasks decremented to: {}", remainingTasks);

            // 立即检查是否满足结束条件
            checkIfAllDone();
        }

        /**
         * 检查是否满足所有结束条件（不修改计数器）
         */
        private void checkIfAllDone() {
            // 必须同时满足两个条件：
            // 1. AI的文本流已经完全结束 (aiStreamFinished == true)
            // 2. 所有已发起的TTS任务都已完成 (pendingTtsTasks.get() == 0)
            if (aiStreamFinished && pendingTtsTasks.get() == 0) {
                log.info("所有条件满足（AI流结束，TTS任务清零），准备向用户发送最终结束信号。");
                sendFinalAudioSignalToUser(this.userId);
            } else {
                log.debug("Check if all done: NO. [aiStreamFinished={}, pendingTasks={}]", aiStreamFinished, pendingTtsTasks.get());
            }
        }

        private int findNextSentenceEnd(String text) {
            for (int i = 0; i < text.length(); i++) {
                if ("，。！？,!?".indexOf(text.charAt(i)) != -1) {
                    return i;
                }
            }
            return -1;
        }
    }

    // ... cleanupSessionResources 和 shutdown 方法保持不变 ...
    private void cleanupSessionResources(String sessionId) {
        log.info("正在清理会话 {} 的所有ASR资源...", sessionId);

        ScheduledFuture<?> task = sendingTasks.remove(sessionId);
        if (task != null) {
            task.cancel(true);
        }

        IflytekAsrClient client = activeClients.remove(sessionId);
        if (client != null && client.isOpen()) {
            client.close();
        }

        BlockingQueue<byte[]> queue = audioQueues.remove(sessionId);
        if (queue != null) {
            queue.clear();
        }

        transcriptBuffers.remove(sessionId);

        log.info("会话 {} 的ASR资源清理完毕。", sessionId);
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
    /**
     * 【核心重构】使用纯响应式方法处理从AI到TTS的完整流程。
     * 此方法定义了一个完整的、非阻塞的数据管道。
     */
    private void handleStreamingAiResponse_Reactive(String fullText, String sessionId, Integer userId) {
        log.info("[业务流] 会话: {}, 用户: {}. 完整识别文本: '{}' -> 即将流式请求AI并转为语音", sessionId, userId, fullText);

        interviewService.interviewProcess(fullText, sessionId, userId)
                // 1. 将AI的流式文本块实时推给前端（用于界面展示）
                .doOnNext(aiChunk -> {
                    if (StringUtils.hasText(aiChunk)) {
                        messagingTemplate.convertAndSendToUser(userId.toString(), DEST_AI_TRANSCRIPT, new ChatMessage("assistant", aiChunk));
                    }
                })
                // 2. 将所有文本块收集成一个完整的字符串
                .collectList()
                .map(chunks -> String.join("", chunks).trim())
                // 3. 【关键修复】使用 flatMap 将一个Mono<String>转换成一个Flux<AudioResponseDto>
                .flatMapMany(fullAiResponse -> {
                    if (fullAiResponse.isEmpty()) {
                        log.warn("AI最终响应为空，不进行TTS合成。SessionID: {}", sessionId);
                        return Flux.empty(); // 如果AI没回复，返回一个空流
                    }

                    log.info("AI完整回复: '{}', 准备进行分句TTS。", fullAiResponse);

                    // 4. 将完整回复按标点切分成句子
                    String[] sentences = fullAiResponse.split("(?<=[" + SENTENCE_DELIMITERS + "])");

                    // 5. 【设计模式改进】使用 Flux.fromArray 和 concatMap 实现TTS的顺序播放
                    // concatMap会保证前一个句子的TTS音频流完全结束后，才开始下一个句子的TTS，确保语音不会重叠。
                    return Flux.fromArray(sentences)
                            .map(String::trim)
                            .filter(StringUtils::hasText)
                            .concatMap(this::reactiveSynthesizeSentence); // 将每个句子响应式地合成为音频流
                })
                // 6. 在所有音频流结束后，追加一个最终的结束信号
                .concatWith(Mono.fromCallable(() -> {
                    log.info("所有TTS任务完成，向用户 {} 发送结束信号。", userId);
                    return new AudioResponseDto(null, true);
                }))
                // 7. 将最终的音频DTO（无论是数据还是结束信号）发送给前端
                .doOnNext(audioDto -> messagingTemplate.convertAndSendToUser(userId.toString(), "/queue/interview/audio.reply", audioDto))
                // 8. 错误处理
                .doOnError(error -> log.error("处理AI->TTS流时发生严重错误。SessionID: {}", sessionId, error))
                // 9. 【关键修复】资源清理。在整个流（包括所有TTS）成功或失败结束后才执行。
                .doFinally(signalType -> cleanupSessionResources(sessionId))
                // 10. 【并发模型】订阅并触发整个流程的执行。
                .subscribe();
    }


    /**
     * 【新增辅助方法】将基于回调的TtsService封装成响应式的Flux。
     * 这是连接异步回调和响应式流的标准桥梁模式。
     *
     * @param sentence 要合成的句子
     * @return 返回一个包含该句子所有音频数据块（Base64）的Flux。
     */
    private Flux<AudioResponseDto> reactiveSynthesizeSentence(String sentence) {
        return Flux.create(sink -> {
            log.info("开始响应式TTS合成: '{}'", sentence);

            // 定义回调
            Consumer<String> onAudioReceived = audioBase64 -> {
                // 每收到一个音频块，就通过sink推送到流中
                sink.next(new AudioResponseDto(audioBase64, false));
            };

            Runnable onComplete = () -> {
                log.info("句子 '{}' 合成完毕。", sentence);
                // TTS合成结束，通知流完成
                sink.complete();
            };

            Consumer<String> onError = errorMsg -> {
                log.error("句子 '{}' 合成失败: {}", sentence, errorMsg);
                // TTS出错，通知流错误
                sink.error(new RuntimeException("TTS synthesis failed: " + errorMsg));
            };

            // 调用旧的、基于回调的TTS服务
            try {
                ttsService.synthesize(sentence, onAudioReceived, onComplete, onError);
            } catch (Exception e) {
                // 捕获立即发生的异常
                sink.error(e);
            }
        });
    }
}