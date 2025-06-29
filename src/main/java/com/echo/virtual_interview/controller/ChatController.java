package com.echo.virtual_interview.controller;

import com.echo.virtual_interview.common.BaseResponse;
import com.echo.virtual_interview.common.ResultUtils;
import com.echo.virtual_interview.context.UserIdContext;
import com.echo.virtual_interview.model.dto.chat.ChatDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;


import com.echo.virtual_interview.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 聊天功能
 */
@RestController
@RequestMapping("/api/chat")
@Slf4j
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * 修改会话名称
     *
     * @param chatId  会话ID
     * @param request 包含新标题的请求体
     * @return 操作成功响应
     */
    @PutMapping("/sessions/{chatId}/title")
    public BaseResponse<Void> updateTitle(@PathVariable String chatId, @RequestBody ChatDTO.UpdateTitleRequest request) {
        Long currentUserId = Long.valueOf(UserIdContext.getUserIdContext());


        chatService.updateChatTitle(chatId, request.getNewTitle(), currentUserId);
        return ResultUtils.success(null);
    }

    /**
     * 删除会话
     *
     * @param chatId 会话ID
     * @return 操作成功响应
     */
    @DeleteMapping("/sessions/{chatId}")
    public BaseResponse<Void> deleteSession(@PathVariable String chatId) {
        Long currentUserId = Long.valueOf(UserIdContext.getUserIdContext());
        chatService.deleteChatSession(chatId, currentUserId);
        return ResultUtils.success(null);
    }
    /**
     * AI对话接口 (流式返回)
     * 支持新建会话或在已有会话中继续。
     *
     * @param request 包含消息和可选的chatId
     * @return SSE 事件流
     */
    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<?>> handleChat(@RequestBody ChatDTO.ChatRequest request) {
        Long currentUserId = Long.valueOf(UserIdContext.getUserIdContext());

        return Flux.concat(
                chatService.processChatStream(request, currentUserId)
                        .map(data -> ServerSentEvent.builder(data).event("message").build()),
                Flux.just(ServerSentEvent.builder().event("end").data("").build())
        );    }

    /**
     * 获取当前用户的所有会话列表
     *
     * @return 会话列表
     */
    @GetMapping("/sessions")
    public BaseResponse<List<ChatDTO.SessionResponse>> getSessions() {
        Long currentUserId = Long.valueOf(UserIdContext.getUserIdContext());
        List<ChatDTO.SessionResponse> sessions = chatService.getChatSessions(currentUserId);
        return ResultUtils.success(sessions);
    }

    /**
     *根据会话ID获取聊天记录
     *
     * @param chatId 会话ID
     * @return 该会话的所有消息列表
     */
    @GetMapping("/sessions/{chatId}")
    public BaseResponse<List<ChatDTO.MessageResponse>> getMessages(@PathVariable String chatId) {
        Long currentUserId = Long.valueOf(UserIdContext.getUserIdContext());
        List<ChatDTO.MessageResponse> messages = chatService.getChatMessages(chatId, currentUserId);
        return ResultUtils.success(messages);
    }

}





/*    *//**
     * 同步调用-测试
     *
     * @param message
     * @param chatId
     * @return
     *//*
    @GetMapping("/chat/sync")
    public String doChatWithLoveAppSync(String message, String chatId) {
        return interviewExpert.dochat(message, chatId);
    }

    *//**
     * SSE 流式调用 AI-测试
     *
     * @param message
     * @param sessionId
     * @return
     *//*
    @GetMapping(value = "/chat/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithLoveAppSSE(String message, String sessionId) {
        return interviewExpert.doChatByStream(message, sessionId);
    }*/
