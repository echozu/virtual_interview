package com.echo.virtual_interview.controller.ws;

import lombok.Data;

/**
 * 用户消息输入 DTO
 */
@Data
public class InterviewMessage {
    private String username; // 用于 convertAndSendToUser
    private String sessionId;
    private String message;
    private String type; // 文本、音频、视频 etc.
    private String payloadUrl; // 如果是视频/音频，可能是一个 OSS URL
}