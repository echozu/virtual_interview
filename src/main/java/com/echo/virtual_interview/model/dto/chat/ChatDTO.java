package com.echo.virtual_interview.model.dto.chat;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

public class ChatDTO {

    /**
     * AI聊天请求体
     */
    @Data
    public static class ChatRequest {
        /**
         * 会话ID。如果为空，则表示新建一个会话。
         */
        private String chatId;

        /**
         * 用户发送的消息内容。
         */
        private String message;

        /**
         * (可选) 本次对话的系统提示词(System Prompt)。
         * 如果提供，则会覆盖本次调用的默认系统提示词。
         */
        private String systemPrompt;
    }

    /**
     * 更新会话标题请求体
     */
    @Data
    public static class UpdateTitleRequest {
        /**
         * 新的会话标题。
         */
        private String newTitle;
    }

    /**
     * 会话列表的响应项
     */
    @Data
    public static class SessionResponse {
        /**
         * 会话ID
         */
        private String chatId;

        /**
         * 会话标题
         */
        private String title;

        /**
         * 最后更新时间
         */
        private LocalDateTime updatedAt;
    }

    /**
     * 单条聊天消息的响应项
     */
    @Data
    public static class MessageResponse {
        /**
         * 消息发送者角色 ('user' 或 'assistant')
         */
        private String role;

        /**
         * 消息内容
         */
        private String content;

        /**
         * 消息创建时间
         */
        private LocalDateTime createdAt;
    }
}
