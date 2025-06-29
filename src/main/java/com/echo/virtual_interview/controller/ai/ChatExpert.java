package com.echo.virtual_interview.controller.ai;

import com.echo.virtual_interview.controller.ai.advisor.MyLoggerAdvisor;
import com.echo.virtual_interview.controller.ai.chatMemory.MysqlChatMemory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import reactor.core.publisher.Flux;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@Component
@Slf4j
public class ChatExpert {
    private final ChatClient chatClient;
    @Autowired
    private final MysqlChatMemory mysqlChatMemory;

    /**
     * 经过优化的新系统提示词
     * 这个提示词为AI设定了明确的角色、语气、行为准则和安全边界，有助于生成更流畅、可靠和一致的回答。
     */
    private static final String SYSTEM_PROMPT = """
            你是一个名为“智能助手”的AI聊天机器人。你的核心任务是为用户提供精准、友好且富有帮助的对话体验。

            请严格遵循以下准则与用户交流：
            1.  **角色与语气**: 你的角色是一位乐于助人的专家伙伴。请始终保持耐心、专业且友好的语气。避免使用过于机械或口语化的表达。
            2.  **清晰与准确**: 运用你的知识库，为用户提供清晰、准确、条理分明的回答。当解释复杂概念时，请善用比喻或分点说明，使其易于理解。
            3.  **诚实与可靠**: 如果你不确定某个问题的答案，或者信息不足，请坦诚地告知用户（例如：“关于这个问题，我目前掌握的信息有限”），绝不臆测或编造不实信息。
            4.  **上下文感知**: 请密切关注对话的上下文，确保你的回答与之前的交流内容连贯、相关。
            5.  **安全与道德**: 严格遵守安全底线，拒绝回答和处理任何涉及暴力、色情、非法活动、歧视或侵犯隐私的危险请求。
            """;

    /**
     * 初始化 通用的ChatClient
     *
     * @param xunFeiChatModel
     */
    public ChatExpert(ChatModel xunFeiChatModel, MysqlChatMemory mysqlChatMemory) {
        this.mysqlChatMemory = mysqlChatMemory;
        this.chatClient = ChatClient.builder(xunFeiChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        // 支持数据库记忆
                        new MessageChatMemoryAdvisor(mysqlChatMemory),
                        // 自定义日志 Advisor
                        new MyLoggerAdvisor()
                        // 增强advisor（即再读一次，耗费token和时间，但更准确）
//                        ,new ReReadingAdvisor()
                )
                .build();
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
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 15))
                .stream()
                .content();
    }

    /**
     * AI 基础对话 支持多轮对话记忆，SSE 流式传输、用户输入提示词
     *
     * @param message
     * @param chatId
     * @return
     */
    public Flux<String> doChatByStream(String message, String chatId,String systemPrompt) {
        return chatClient
                .prompt(systemPrompt)
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 15))
                .stream()
                .content();
    }
    /**
     * 面试对话-非流式
     * （带10条上下文记忆）
     *
     * @param message   用户信息
     * @param chatId 会话id
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
        // 得到ai的回答, .getText() 在新版中已不推荐使用，推荐使用 .getContent()
        String content = response.getResult().getOutput().getText();
        log.info("AI Response: {}", content);
        return content;
    }

}
