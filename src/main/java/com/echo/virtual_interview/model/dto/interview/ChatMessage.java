package com.echo.virtual_interview.model.dto.interview;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    private String sender;   // 发送者：'user' (用户) 或 'assistant
    private String content;  // 消息内容
}
