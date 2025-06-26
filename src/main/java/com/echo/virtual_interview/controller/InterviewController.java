package com.echo.virtual_interview.controller;

import com.echo.virtual_interview.common.BaseResponse;
import com.echo.virtual_interview.common.ResultUtils;
import com.echo.virtual_interview.context.UserIdContext;
import com.echo.virtual_interview.controller.ai.InterviewExpert;
import com.echo.virtual_interview.service.IInterviewService;
import com.echo.virtual_interview.service.IInterviewSessionsService;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.http.MediaType;
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

    @Resource
    private InterviewExpert interviewExpert;
    @Resource
    private IInterviewService interviewService;

    @Resource
    private IInterviewSessionsService interviewSessionsService;
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
     * 开启面试
     *
     * @param channelId 所选中的频道ID (required)
     * @return 包含sessionId的响应实体
     */
    @GetMapping("/chat/start")
    public BaseResponse<String> interviewStart(
            @RequestParam Long channelId) {
        String sessionId = interviewSessionsService.start(channelId);
        return ResultUtils.success(sessionId);

    }

    /**
     * 面试过程
     * @param message 用户信息
     * @param sessionId 聊天唯一id
     * @return 返回信息
     */
    @GetMapping(value = "/chat/send", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> interviewProcess(String message, String sessionId) {

        return interviewService.interviewProcess(message, sessionId);
    }
}
