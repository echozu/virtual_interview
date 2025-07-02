package com.echo.virtual_interview.controller;

import com.echo.virtual_interview.context.UserIdContext;
import com.echo.virtual_interview.controller.ai.InterviewExpert;
import com.echo.virtual_interview.model.dto.interview.audio.AudioChunkDto;
import com.echo.virtual_interview.model.dto.interview.ChatMessage;
import com.echo.virtual_interview.service.IInterviewService;
import com.echo.virtual_interview.service.IInterviewSessionsService;
import com.echo.virtual_interview.service.impl.AsrProcessingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Base64;

/**
 * 专门处理面试相关的WebSocket消息
 */
@Controller
@Slf4j
public class InterviewWsController {

    private final IInterviewService interviewService;
    private final AsrProcessingService asrProcessingService;
    private final IInterviewSessionsService interviewSessionsService;
    private final SimpMessagingTemplate messagingTemplate;
    private final InterviewExpert interviewExpert;

    // --- 最佳实践：使用一个构造函数完成所有依赖注入 ---
    public InterviewWsController(IInterviewService interviewService,
                               IInterviewSessionsService interviewSessionsService,
                               SimpMessagingTemplate messagingTemplate,
                               AsrProcessingService asrProcessingService, InterviewExpert interviewExpert) {
        this.interviewService = interviewService;
        this.interviewSessionsService = interviewSessionsService;
        this.messagingTemplate = messagingTemplate;
        this.asrProcessingService = asrProcessingService;
        this.interviewExpert = interviewExpert;
    }

    /**语音--文本（ai）---语音
     * 处理前端发送的音频流数据，并返回流式音频
     * 现在接收一个包含Base64音频字符串的DTO
     */
    @MessageMapping("/interview/process/audio/tts/{sessionId}")
    public void handleAudioChunkTTS(
            @DestinationVariable String sessionId,
            @Payload AudioChunkDto chunkDto, //
            @Header("user-id") String userIdStr) {
        try {
            Integer userId = Integer.parseInt(userIdStr);

            // --- 新增：从Base64解码回原始的byte[] ---
            byte[] audioData = Base64.getDecoder().decode(chunkDto.getAudio());

            log.trace("收到来自用户 {} 会话 {} 的音频数据，解码后大小: {} bytes", userId, sessionId, audioData.length);
            asrProcessingService.processAudioChunkTTS(sessionId, userId, audioData);
        } catch (NumberFormatException e) {
            log.error("无效的用户ID格式: {}", userIdStr);
        } catch (IllegalArgumentException e) {
            // 如果Base64字符串格式不正确，会抛出此异常
            log.error("Base64解码音频数据失败: {}", e.getMessage());
        }
    }
    /**
     * 处理前端发送的音频结束信号
     * 前端应发送到: /api/interview/process/audio/end/{sessionId}
     */
    @MessageMapping("/interview/process/audio/end/{sessionId}")
    public void handleAudioEnd(
            @DestinationVariable String sessionId,
            @Header("user-id") String userIdStr) {
        log.info("收到用户 {} 会话 {} 的音频结束信号", userIdStr, sessionId);
        asrProcessingService.endAudioStream(sessionId);
    }

    /**
     * 面试过程-websocket-文字版 -测试
     * 注意：为了与音频接口保持一致，路径最好也统一
     */
    @MessageMapping("/interview/process/{sessionId}")
    public void handleUserMessage(
            @DestinationVariable String sessionId,
            @Payload ChatMessage message,
            @Header("user-id") String userIdStr) {
        try {
            Integer userId = Integer.parseInt(userIdStr);
            UserIdContext.setUserIdContext(userId);

            interviewService.interviewProcess(message.getContent(), sessionId, userId)
                    .subscribe(reply -> {
                        messagingTemplate.convertAndSendToUser(
                                userId.toString(),
                                "/queue/interview/answer", // 与前端订阅地址匹配
                                new ChatMessage("assistant", reply)
                        );
                    });
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("非法的用户ID格式");
        }
    }

    /**
     * 处理前端发送的音频流数据，并返回流式文本 -测试
     * 现在接收一个包含Base64音频字符串的DTO
     */
    @MessageMapping("/interview/process/audio/{sessionId}")
    public void handleAudioChunk(
            @DestinationVariable String sessionId,
            @Payload AudioChunkDto chunkDto, // <-- 修改点：使用DTO接收
            @Header("user-id") String userIdStr) {
        try {
            Integer userId = Integer.parseInt(userIdStr);

            // --- 新增：从Base64解码回原始的byte[] ---
            byte[] audioData = Base64.getDecoder().decode(chunkDto.getAudio());

            log.trace("收到来自用户 {} 会话 {} 的音频数据，解码后大小: {} bytes", userId, sessionId, audioData.length);
            asrProcessingService.processAudioChunk(sessionId, userId, audioData);
        } catch (NumberFormatException e) {
            log.error("无效的用户ID格式: {}", userIdStr);
        } catch (IllegalArgumentException e) {
            // 如果Base64字符串格式不正确，会抛出此异常
            log.error("Base64解码音频数据失败: {}", e.getMessage());
        }
    }

}

/*
* WebSocket连接地址 (不变):
前端依然通过 ws://localhost:9527/ws/interview 这个地址发起WebSocket连接。

音频发送地址 (不变):
前端依然向 /api/interview/process/audio/{sessionId} 这个目标地址发送音频数据。
* */