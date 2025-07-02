package com.echo.virtual_interview.controller.ai;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class ChatExpertTest {
    @Resource
    private ChatExpert chatExpert;

    @Test
    void doChatWithRag() {
        String chatId = UUID.randomUUID().toString();
        // 第一轮
        String message = "你好，我叫什么名字";
        String answer = chatExpert.doChatWithRag(message, chatId);
        System.out.println(answer);
    }
}