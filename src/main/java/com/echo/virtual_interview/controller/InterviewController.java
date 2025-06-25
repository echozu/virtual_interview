package com.echo.virtual_interview.controller;

import com.echo.virtual_interview.controller.ai.InterviewExpert;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
    private ChatModel chatModel;
    @Resource
    private InterviewExpert interviewExpert;

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
     * SSE 流式调用 AI 恋爱大师应用
     *
     * @param message
     * @param chatId
     * @return
     */
    @GetMapping(value = "/chat/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithLoveAppSSE(String message, String chatId) {
        return interviewExpert.doChatByStream(message, chatId);
    }
}
