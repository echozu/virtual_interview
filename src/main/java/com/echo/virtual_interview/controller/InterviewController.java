package com.echo.virtual_interview.controller;

import com.echo.virtual_interview.common.BaseResponse;
import com.echo.virtual_interview.common.ResultUtils;
import com.echo.virtual_interview.context.UserIdContext;
import com.echo.virtual_interview.controller.ai.InterviewExpert;
import com.echo.virtual_interview.model.dto.interview.ChatMessage;
import com.echo.virtual_interview.service.IInterviewService;
import com.echo.virtual_interview.service.IInterviewSessionsService;
import com.echo.virtual_interview.utils.rtasr.AsrProcessingService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * 面试接口
 */
@RestController
@RequestMapping("/api/interview")
@Slf4j
public class InterviewController {

    private final IInterviewService interviewService;
    private final IInterviewSessionsService interviewSessionsService;
    private final SimpMessagingTemplate messagingTemplate;
    private final AsrProcessingService asrProcessingService;
    private final InterviewExpert interviewExpert;

    // --- 最佳实践：使用一个构造函数完成所有依赖注入 ---
    public InterviewController(IInterviewService interviewService,
                               IInterviewSessionsService interviewSessionsService,
                               SimpMessagingTemplate messagingTemplate,
                               AsrProcessingService asrProcessingService, InterviewExpert interviewExpert) {
        this.interviewService = interviewService;
        this.interviewSessionsService = interviewSessionsService;
        this.messagingTemplate = messagingTemplate;
        this.asrProcessingService = asrProcessingService;
        this.interviewExpert = interviewExpert;
    }


    /**
     * 开启面试
     * 这里去创建一个新的session，方便接下来的面试存储
     *
     * @param channelId 所选中的频道ID (required)
     * @return 包含sessionId的响应实体
     */
    @GetMapping("/chat/start")
    public BaseResponse<String> interviewStart(@RequestParam Long channelId) {
        String sessionId = interviewSessionsService.start(channelId);
        return ResultUtils.success(sessionId);
    }





    /**
     * 同步调用
     *
     * @param message
     * @param chatId
     * @return
     */
    @GetMapping("/chat/sync")
    public String doChatWithLoveAppSync(String message, String chatId) {
        return interviewExpert.dochat(message, chatId);
    }

    /**
     * SSE 流式调用 AI
     *
     * @param message
     * @param chatId
     * @return
     */
    @GetMapping(value = "/chat/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithLoveAppSSE(String message, String chatId) {
        return interviewExpert.doChatByStream(message, chatId);
    }

    /**
     * 面试过程-sse
     *
     * @param message   用户信息
     * @param sessionId 聊天唯一id
     * @return 返回信息
     */
    @GetMapping(value = "/chat/send", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> interviewProcess(String message, String sessionId) {
        // 获取当前登录用户
        Integer userId = UserIdContext.getUserIdContext();
        return interviewService.interviewProcess(message, sessionId, userId);
    }


}
