package com.echo.virtual_interview.controller.ai;

import com.echo.virtual_interview.controller.ai.advisor.MyLoggerAdvisor;
import com.echo.virtual_interview.controller.ai.chatMemory.MysqlChatMemory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import reactor.core.publisher.Flux;

import java.util.List;

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
    public ChatExpert(ChatModel xunFeiChatModel, MysqlChatMemory mysqlChatMemory, Advisor ragCloudAdvisor) {
        this.mysqlChatMemory = mysqlChatMemory;
        this.ragCloudAdvisor = ragCloudAdvisor;
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
    public Flux<String> doChatByStream(String message, String chatId, String systemPrompt) {
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
        // 得到ai的回答, .getText() 在新版中已不推荐使用，推荐使用 .getContent()
        String content = response.getResult().getOutput().getText();
        log.info("AI Response: {}", content);
        return content;
    }


    @Qualifier("RagCloudAdvisor")
    private final Advisor ragCloudAdvisor;

    /**
     * 通过RAG（检索增强生成）与AI进行对话。
     * 这个方法集成了外部知识库（讯飞）来为大模型提供回答问题的上下文。
     *
     * @param message 用户的原始提问
     * @param chatId  用于多轮对话的会话ID
     * @return 结合了知识库内容后，由大模型生成的最终回答
     */
    public String doChatWithRag(String message, String chatId) {
        log.info("开始使用RAG进行对话，用户问题: {}", message);

        // 整个流程始于 ChatClient，它是与大模型交互的统一入口。
        // .prompt() 方法开始构建一个完整的调用请求。
        ChatResponse chatResponse = chatClient.prompt()
                .user(message) // 设置用户的原始输入

                // .advisors(...) 是实现RAG的核心，它允许我们在请求发送给大模型之前对其进行拦截和增强。
                // 我们在这里插入自定义的 Advisor，它封装了“检索”这一步骤。
                .advisors(spec ->  spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 15)  // 这个是检索多少条数据库
                        // 这里可以链式添加多个 Advisor，例如处理聊天记录的、记录日志的等。
                        // .param(...) 用于向 Advisor 传递参数，例如向聊天记录Advisor传递会话ID。
                        // .advisor(chatMemoryAdvisor)

                        // 【核心步骤1: 调用知识库】
                        // 当 .call() 被触发时，Spring AI会执行我们配置的 ragCloudAdvisor (其实例为 RetrievalAugmentationAdvisor)。
                        // 这个 Advisor 内部会执行以下操作：
                        //   a. 调用我们自定义的 xfyunSparkKnowledgeRetriever (它实现了 DocumentRetriever 接口)。
                        //   b. xfyunSparkKnowledgeRetriever 会向讯飞的知识库API发起HTTP请求，进行向量相似度搜索，
                        //      从而根据用户的 message 找回最相关的文档片段 (List<Document>)。
                        .advisors(ragCloudAdvisor)
                )

                // .call() 方法是流程的触发器。
                // 它会首先执行所有已注册的 Advisor (如 ragCloudAdvisor)，然后将 Advisor 处理和增强后的最终Prompt发送给大模型。
                .call()
                .chatResponse(); // 获取包含完整响应信息（包括元数据）的 ChatResponse 对象

        // 从响应中获取大模型生成的最终文本内容
        String content = chatResponse.getResult().getOutput().getText();
        log.info("模型结合知识库后的最终回答: {}", content);

        // 【核心步骤2: 检查检索结果】
        // RetrievalAugmentationAdvisor 在执行完检索后，会将检索到的文档列表放入响应的元数据(metadata)中。
        // 存放的Key是 RetrievalAugmentationAdvisor.DOCUMENT_CONTEXT ("rag_document_context")。
        // 所以我们不能用 getRetrievedDocuments()，而是要从 map 中通过这个固定的 key 来获取。

        // 从元数据中获取检索到的文档列表，需要进行类型转换和空指针检查。
        @SuppressWarnings("unchecked")
        List<Document> retrievedDocuments = (List<Document>) chatResponse.getMetadata()
                .get(RetrievalAugmentationAdvisor.DOCUMENT_CONTEXT);

        if (retrievedDocuments != null && !retrievedDocuments.isEmpty()) {
            log.info("本次问答检索到了 {} 篇相关文档。", retrievedDocuments.size());
            // 遍历打印每个文档块的内容和它的相关性分数（如果存在）
            retrievedDocuments.forEach(doc -> {
                // 分数通常也保存在每个Document自己的元数据中
                Object score = doc.getMetadata().get("score");
                log.debug(" - [相关性分数: {}] 内容: {}", score, doc.getText());
            });
        } else {
            log.info("本次问答未从知识库中检索到相关文档。");
        }

        return content;
    }

}
