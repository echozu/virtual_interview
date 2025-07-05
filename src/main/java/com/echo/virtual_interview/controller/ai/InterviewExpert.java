package com.echo.virtual_interview.controller.ai;

import com.echo.virtual_interview.adapater.ResumeAndChannelAdapter;
import com.echo.virtual_interview.controller.ai.advisor.MyLoggerAdvisor;
import com.echo.virtual_interview.controller.ai.chatMemory.MysqlInterviewMemory;
import com.echo.virtual_interview.model.dto.interview.channel.ChannelDetailDTO;
import com.echo.virtual_interview.model.dto.interview.process.RealtimeFeedbackDto;
import com.echo.virtual_interview.model.dto.resum.ResumeDataDto;
import com.echo.virtual_interview.model.entity.ResumeModule;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
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
    @Resource
    private ObjectMapper objectMapper;
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

    // 根据视频流+图片的分析
    /**
     * 专为实时视频+图片分析设计的提示词。
     * 它的目标是：快速、简洁、聚焦于核心状态。
     */
    /**
     * 专为实时视频+图片分析设计的提示词。
     * V2版本：包含了详细的数据释义，并要求AI以JSON格式返回结构化数据。
     */

    /**
     * 专为实时视频+图片分析设计的提示词。
     * V3版本：在返回的JSON中增加了 detailed_analysis 字段，用于数据库存储。
     */
    /**
     * 专为实时视频+图片分析设计的提示词。
     * V3版本：增加了更多维度的输入数据和更详细的数据释义，以提升AI分析的准确性。
     */
    private static final String REALTIME_VIDEO_PICTURE_ANALYSIS_PROMPT = """
            你是一位顶级的AI面试行为分析官，极其擅长从细微的非语言信号中解读候选人的心理状态。
            你的任务是根据以下提供的候选人实时行为数据，用中文生成一份结构化的即时状态反馈。

            # 核心规则
            1.  **聚焦核心状态**：你的分析必须严格围绕候选人的 **眼神交流质量** 和 **情绪紧张程度** 展开。
            2.  **循证分析**：你的每一句结论都必须基于我提供的数据，不允许凭空想象。
            3.  **专业且简洁**：语言要专业、精炼，符合面试辅导的场景。
            4.  **JSON输出**：必须严格按照我要求的JSON格式输出，不要包含任何Markdown标记或额外文字。

            # 数据释义 (这是你分析的依据)
            - `核心紧张度` (String): 对紧张状态的直接定性，如 "高度紧张", "状态放松"。
            - `平均注意力分数` (Number): 眼神接触质量的核心指标。分数 > 0.9 表示非常专注；0.7-0.9 表示基本专注；< 0.7 表示注意力有明显分散。
            - `注意力稳定性` (Number): 眼神飘忽程度。该值(标准差) > 0.3 暗示眼神游离，是焦虑的信号。
            - `总眨眼次数` (Number): 紧张的生理反应。半分钟内 > 20次 是典型的压力信号。
            - `头部晃动幅度` (Number): 不安的小动作。该值(偏航角标准差) > 4.0 可能与焦虑有关。
            - `开场表情` / `当前表情` (String): 面部表情的快照，是判断情绪的直接证据。

            # 输入数据
            {ANALYSIS_DATA}

            # 输出要求 (必须是可直接解析的JSON)
            请生成一个包含以下字段的JSON对象：
            - `summary` (String): 一句话总结候选人当前的状态，用于实时显示给用户。
            - `suggestion` (String): 如果状态不佳，提供一句具体的、可操作的改进建议。如果状态良好，此字段为空字符串。
            - `status` (String): 状态标识符，只能是 "POSITIVE" (状态良好) 或 "NEEDS_IMPROVEMENT" (有待提高) 中的一个。
            - `detailed_analysis` (Object): 一个包含详细分析条目的对象，用于后台存储。其中应包含 `eye_contact_analysis` 和 `emotional_state_analysis` 两个键，内容是你对这两方面表现的详细文字说明。

            [你的JSON输出]
            """;

    /**
     * 根据格式化的视频流和图片分析数据，调用AI生成实时反馈。
     *
     * @param formattedAnalysisData 经过处理和格式化的分析数据字符串。
     * @return AI生成的简短实时反馈。
     */
    /**
     * 根据格式化的视频流和图片分析数据，调用AI生成结构化的实时反馈。
     */
    public RealtimeFeedbackDto aiInterviewByVideoAndPicture(String formattedAnalysisData) {
        String finalPrompt = REALTIME_VIDEO_PICTURE_ANALYSIS_PROMPT.replace("{ANALYSIS_DATA}", formattedAnalysisData);
        try {
        return  this.chatClient.
                prompt()
                .user(finalPrompt)
                .call()
                .entity(RealtimeFeedbackDto.class);
        } catch (Exception e) {
            log.error("解析AI返回的实时反馈JSON失败");
            return new RealtimeFeedbackDto("AI分析结果解析失败，请稍后重试。", "", "NEEDS_IMPROVEMENT", null);
        }
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
