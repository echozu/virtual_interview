package com.echo.virtual_interview.controller.ai;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

@SpringBootTest
class InterviewExpertTest {

    @Resource
    private InterviewExpert interviewExpert;

    @Test
    void testMultiTurnChat() {
        String chatId = UUID.randomUUID().toString();

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

//    @Test
//    void testChatWithReport() {
//        String chatId = UUID.randomUUID().toString();
//        String message = "我在面试中经常被问到关于项目优化的问题，比如如何提升接口性能。";
//
//        InterviewExpert.InterviewReport report = interviewExpert.askWithReport(message, chatId);
//        Assertions.assertNotNull(report);
//        Assertions.assertNotNull(report.title());
//        Assertions.assertFalse(report.suggestions().isEmpty());
//
//        System.out.println("面试报告标题：" + report.title());
//        System.out.println("建议列表：");
//        report.suggestions().forEach(System.out::println);
//    }
}
