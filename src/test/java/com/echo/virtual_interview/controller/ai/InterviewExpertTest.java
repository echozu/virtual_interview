package com.echo.virtual_interview.controller.ai;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

@SpringBootTest
class InterviewExpertTest {

    @Resource
    private InterviewExpert interviewExpert;

    @Test
    void testMultiTurnChat() {
        String chatId = "1";

        // 第一轮
        String message1 = "你好，我是echo。";
        String answer1 = interviewExpert.dochat(message1, chatId);
        Assertions.assertNotNull(answer1);
        System.out.println("回答1：" + answer1);

        // 第二轮
//        String message2 = "我经常在自我介绍时卡壳，不知道怎么讲项目亮点。";
//        String answer2 = interviewExpert.ask(message2, chatId);
//        Assertions.assertNotNull(answer2);
//        System.out.println("回答2：" + answer2);

        // 第三轮
        String message3 = "现在我问你我叫什么名字，你只需要回答我的名字？";
        String answer3 = interviewExpert.dochat(message3, chatId);
        Assertions.assertNotNull(answer3);
        System.out.println("回答3：" + answer3);
    }

    @Test
    void testSse() {
        String chatId = UUID.randomUUID().toString();
        String message = "你好，我是echo，我想知道我在自我介绍中怎么更有逻辑。";

        Flux<String> responseFlux = interviewExpert.doChatByStream(message, chatId);

        Assertions.assertNotNull(responseFlux);

        System.out.println("【流式开始】");

        // 监听每段内容并打印出来
        responseFlux
                .doOnNext(chunk -> System.out.println("🧠 AI响应片段: " + chunk))
                .doOnComplete(() -> System.out.println("✅ 流式响应结束"))
                .doOnError(error -> System.err.println("❌ 出错：" + error.getMessage()))
                .blockLast(); // 阻塞直到流结束
    }


}
