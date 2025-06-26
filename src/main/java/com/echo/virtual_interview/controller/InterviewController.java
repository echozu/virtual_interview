package com.echo.virtual_interview.controller;

import com.echo.virtual_interview.common.BaseResponse;
import com.echo.virtual_interview.common.ResultUtils;
import com.echo.virtual_interview.context.UserIdContext;
import com.echo.virtual_interview.controller.ai.InterviewExpert;
import com.echo.virtual_interview.model.dto.interview.ChatMessage;
import com.echo.virtual_interview.service.IInterviewService;
import com.echo.virtual_interview.service.IInterviewSessionsService;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.security.Principal;

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

    @Autowired
    private final SimpMessagingTemplate messagingTemplate;
    public InterviewController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }
    /**
     * 面试过程-websocket
     */
    @MessageMapping("/ws/chat/{sessionId}")
    public void handleUserMessage(
            @DestinationVariable String sessionId,
            @Payload ChatMessage message,
            @Header("user-id") String userIdStr) {

        try {
            Integer userId = Integer.parseInt(userIdStr); // 显式转换
            UserIdContext.setUserIdContext(userId);

            interviewService.interviewProcess(message.getContent(), sessionId, userId)
                    .subscribe(reply -> {
                        messagingTemplate.convertAndSendToUser(
                                userId.toString(), // 决定要发给谁：这个就是标识，如果是填sessionid，则会发送给所有监听该sessionid的多个用户
                                "/interview/answer", //这里决定通道类型：这里表示发送给发送者自己，前端需要订阅这个地址，去接收
                                new ChatMessage("AI", reply)
                        );
                    });
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("非法的用户ID格式");
        }
    }

/*   前端：用户123的连接需要订阅：
stompClient.subscribe('/user/interview/answer', (message) => {
        console.log("收到AI回复:", message.body);
    });

实际监听的物理地址： /user/123/interview/answer*/
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
     * 面试过程-sse
     * @param message 用户信息
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
