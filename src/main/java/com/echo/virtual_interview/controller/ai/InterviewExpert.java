package com.echo.virtual_interview.controller.ai;

import com.echo.virtual_interview.controller.ai.advisor.MyLoggerAdvisor;
import com.echo.virtual_interview.controller.ai.advisor.ReReadingAdvisor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@Component
@Slf4j
public class InterviewExpert {

    private final ChatClient chatClient;

    private static final String SYSTEM_PROMPT = """
            你是一位专精于技术类岗位面试辅导的AI教练，具备丰富的大厂面试经验。
            - 识别内容阶段（如：自我介绍 / 项目经历 / 技术问答 / 行为问题等）；
            - 分析表达逻辑，指出亮点与待改进点；
            - 引导用户补充细节，使用 STAR 法；
            - 多轮总结建议；
            - 最终生成“面试反馈报告”。
            请保持专业、鼓励性的语气，引导用户逐步提升。
            """;
    /**
     * 初始化 通用的ChatClient
     *
     * @param xunFeiChatModel
     */
    public InterviewExpert(ChatModel xunFeiChatModel) {
        //todo:基于内存的记忆，这里可以改为mysql的
        ChatMemory chatMemory = new InMemoryChatMemory();
        this.chatClient = ChatClient.builder(xunFeiChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        // 内存记忆
                        new MessageChatMemoryAdvisor(chatMemory),
                        // 自定义日志 Advisor
                        new MyLoggerAdvisor()
                        // 增强advisor（即再读一次，耗费token和时间，但更准确）
                        ,new ReReadingAdvisor()
                )

                .build();
    }

    /**
     * 面试对话-非流式
     * （带10条上下文记忆）
     * @param message 用户信息
     * @param chatId  会话id
     * @return
     */
    public String dochat(String message, String chatId) {
        ChatResponse response = chatClient.prompt()
                // 传入用户的问题
                .user(message)
                // 这里再定义advisor顾问：这里设置10条上下文记忆：这里可以获得最近的n条消息是因为去手动实现了
                // List<Message> get(String conversationId, int lastN);，然后这里是自动调用
                .advisors(spec -> spec
                        .param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .call()
                .chatResponse();
        // 得到ai的回答
        String content = response.getResult().getOutput().getText();
        log.info("AI Response: {}", content);
        return content;
    }
    /**
     * AI 基础对话（支持多轮对话记忆，SSE 流式传输）
     *
     * @param message
     * @param chatId
     * @return
     */
    public Flux<String> doChatByStream(String message, String chatId) {
        return chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .stream()
                .content();
    }


    public record InterviewReport(String title, List<String> suggestions) {
    }
}
