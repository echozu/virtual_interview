package com.echo.virtual_interview.controller.ai;

import com.echo.virtual_interview.adapater.ResumeAndChannelAdapter;
import com.echo.virtual_interview.controller.ai.advisor.MyLoggerAdvisor;
import com.echo.virtual_interview.controller.ai.chatMemory.MysqlInterviewMemory;
import com.echo.virtual_interview.model.dto.interview.channel.ChannelDetailDTO;
import com.echo.virtual_interview.model.dto.resum.ResumeDataDto;
import com.echo.virtual_interview.model.entity.ResumeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Optional;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@Component
@Slf4j
public class InterviewExpert {

    private final ChatClient chatClient;
    @Autowired
    private final MysqlInterviewMemory mysqlInterviewMemory;
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
    public InterviewExpert(ChatModel xunFeiChatModel, MysqlInterviewMemory mysqlInterviewMemory, Advisor ragCloudAdvisor) {
        this.mysqlInterviewMemory = mysqlInterviewMemory;
        this.ragCloudAdvisor = ragCloudAdvisor;
        this.chatClient = ChatClient.builder(xunFeiChatModel)
//                .defaultSystem(SYSTEM_PROMPT)  // 这里先不需要基础的聊天提示词，后面的client自己加即可
                .defaultAdvisors(
                        // 支持数据库记忆
                        new MessageChatMemoryAdvisor(mysqlInterviewMemory),
                        // 自定义日志 Advisor
                        new MyLoggerAdvisor()
                        // 增强advisor（即再读一次，耗费token和时间，但更准确）
//                        ,new ReReadingAdvisor()
                )

                .build();
    }
    @Qualifier("RagCloudAdvisor")
    private final Advisor ragCloudAdvisor; // 添加讯飞的知识库
    /**
     * 面试过程的对话
     *
     * @param message
     * @param chatId
     * @return
     */
    public Flux<String> aiInterviewByStreamWithProcess(
            String message,
            String chatId,
            ResumeDataDto resume,
            List<ResumeModule> resumeModules,
            ChannelDetailDTO channel) {

        // 使用适配器格式化信息
        ResumeAndChannelAdapter adapter = new ResumeAndChannelAdapter();
        String formattedResume = adapter.formatResumeToMarkdown(resume, resumeModules);
        String formattedChannel = adapter.formatChannelToMarkdown(channel);

        String systemContent = """
        你是一位%s的AI面试官，正在为【%s】公司招聘【%s】岗位进行模拟面试。
        
        【面试阶段指引】
        1. 开场阶段：要求候选人用1分钟自我介绍（未完成时提示："请先简要介绍自己，包括技术栈和最近项目"）
        2. 深度追问：当候选人回答包含以下内容时追问：
           - 🔍 技术关键词（如Java/MySQL）："你在这个项目中具体如何应用%s？"
           - 📈 未量化结果："这个优化具体提升了多少性能指标？"
           - ⏱️ 时间矛盾："简历显示该项目周期2周，但您说完成了XX功能，时间如何分配？"
        
        【当前背景】
        ==== 候选人简历 ====
        %s
        
        ==== 面试配置 ====
        %s
        
        【应答策略】
        根据对话历史选择最合适的响应方式：
        ▶️ 追问细节（当回答存在技术深度可挖）
        ▶️ 质疑矛盾（当发现简历与表述不一致）
        ▶️ 切换方向（当前话题已充分讨论）
        ▶️ 给予反馈（回答质量变化时）
        
        请用以下格式响应：
        💡 策略：[追问/质疑/切换/反馈]
        🎤 内容：（严格控制在2-3句话内）
        """.formatted(
                channel.getInterviewerStyle(),
                Optional.ofNullable(channel.getTargetCompany()).orElse("目标公司"),
                Optional.ofNullable(channel.getTargetPosition()).orElse("技术岗位"),
                getMainSkill(resumeModules), // 从简历模块提取核心技能
                formattedResume,
                formattedChannel
        );

        return chatClient.prompt()
                .system(systemContent)
                .user(message)
                .advisors(spec -> spec
                        .param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10)
                        .advisors(ragCloudAdvisor)  // 这里是开启讯飞知识库
                )
                .stream()
                .content();
    }

    // 辅助方法：从简历模块提取核心技能
    private String getMainSkill(List<ResumeModule> modules) {
        return modules.stream()
                .filter(m -> "SKILLS".equals(m.getModuleType()))
                .findFirst()
                .map(ResumeModule::getContent)
                .orElse("相关技术");
    }







/*    *//**
     * 面试对话-非流式（未用）
     * （带10条上下文记忆）
     *
     * @param message 用户信息
     * @param sessionId  会话id
     * @return
     *//*
    public String dochat(String message, String sessionId) {
        ChatResponse response = chatClient.prompt()
                // 传入用户的问题
                .user(message)
                // 这里再定义advisor顾问：这里设置10条上下文记忆：这里可以获得最近的n条消息是因为去手动实现了
                // List<Message> get(String conversationId, int lastN);，然后这里是自动调用
                .advisors(spec -> spec
                        .param(CHAT_MEMORY_CONVERSATION_ID_KEY, sessionId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .call()
                .chatResponse();
        // 得到ai的回答
        String content = response.getResult().getOutput().getText();
        log.info("AI Response: {}", content);
        return content;
    }

    *//**
     * AI 基础对话（支持多轮对话记忆，SSE 流式传输）（未用）
     *
     * @param message
     * @param sessionId
     * @return
     *//*
    public Flux<String> doChatByStream(String message, String sessionId) {
        return chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, sessionId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 15))
                .stream()
                .content();
    }*/
}
