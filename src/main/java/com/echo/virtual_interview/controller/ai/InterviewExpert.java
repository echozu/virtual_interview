package com.echo.virtual_interview.controller.ai;

import com.echo.virtual_interview.adapater.ResumeAndChannelAdapter;
import com.echo.virtual_interview.controller.ai.advisor.MyLoggerAdvisor;
import com.echo.virtual_interview.controller.ai.chatMemory.MysqlInterviewMemory;
import com.echo.virtual_interview.model.dto.interview.AnalysisReportDTO;
import com.echo.virtual_interview.model.dto.interview.TurnAnalysisResponse;
import com.echo.virtual_interview.model.dto.interview.channel.ChannelDetailDTO;
import com.echo.virtual_interview.model.dto.interview.process.RealtimeFeedbackDto;
import com.echo.virtual_interview.model.dto.resum.ResumeDataDto;
import com.echo.virtual_interview.model.entity.ResumeModule;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    @Qualifier("RagCloudAdvisor")
    private final Advisor ragCloudAdvisor; // 添加讯飞的知识库

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

                        // 自定义日志 Advisor
                        new MyLoggerAdvisor()
                        // 增强advisor（即再读一次，耗费token和时间，但更准确）
//                        ,new ReReadingAdvisor()
                )

                .build();
    }

    // 根据视频流+图片的分析

    /**
     * 面试过程：再次分析视频情况+图片。
     */
    public RealtimeFeedbackDto aiInterviewByVideoAndPicture(String formattedAnalysisData) {
        String finalPrompt = REALTIME_VIDEO_PICTURE_ANALYSIS_PROMPT.replace("{ANALYSIS_DATA}", formattedAnalysisData);
        log.info("面试时输入给ai的最终提示词：{}", finalPrompt);
        // 1. 获取AI返回的原始字符串
        String rawResponse = this.chatClient.prompt()
                .user(finalPrompt)
                .call()
                .content();
        try {
            // 2. 使用工具类提取出干净的JSON部分
            String extractedJson = JsonExtractor.extractJson(rawResponse);

            if (extractedJson == null) {
                log.error("无法从AI响应中提取出JSON结构。原始响应: {}", rawResponse);
                return new RealtimeFeedbackDto("AI响应格式错误，无法解析。", "", "NEEDS_IMPROVEMENT", null);
            }

            // 3. 直接将提取出的JSON字符串反序列化为RealtimeFeedbackDto对象
            RealtimeFeedbackDto feedbackDto = objectMapper.readValue(extractedJson, RealtimeFeedbackDto.class);

            log.info("成功解析AI的分析结果。");
            return feedbackDto;

        } catch (Exception e) {
            // 4. 如果任何步骤出错，记录异常并返回一个明确的错误DTO
            log.error("处理AI响应时发生严重错误。", e);
            return new RealtimeFeedbackDto("AI分析结果解析失败，请稍后重试。", "", "NEEDS_IMPROVEMENT", rawResponse);
        }
    }
    // 面试过程的对话分析

    /**
     * 面试过程：进行面试对话时的交流。
     *
     * @param message       用户的最新消息。
     * @param chatId        会话记忆的唯一 ID。
     * @param resume        候选人的简历数据。
     * @param resumeModules 简历的结构化模块。
     * @param channel       当前面试的配置。
     * @return 表示 AI 回应的字符串流。
     */
    public Flux<String> aiInterviewByStreamWithProcess(
            String message,
            String chatId,
            ResumeDataDto resume,
            List<ResumeModule> resumeModules,
            ChannelDetailDTO channel) {

        // 使用适配器格式化上下文信息
        ResumeAndChannelAdapter adapter = new ResumeAndChannelAdapter();
        String formattedResume = adapter.formatResumeToMarkdown(resume, resumeModules);
        String formattedChannel = adapter.formatChannelToMarkdown(channel);

        // ✅ --- V3: 最终、更健壮的系统提示（已调整为适合流式输出）---
        String systemContent = """
                # 任务说明
                你是一位世界级的 AI 技术面试官。你的主要目标是进行高度真实、深入且公平的技术面试，以全面评估候选人的技能和经验。
                
                # 角色设定
                - **风格**: 你要扮演 "%s"。你应当专业、客观、有洞察力，并且能鼓励候选人。你的目标是挖掘候选人的真实能力，而不是刁难他们。
                
                # 面试背景
                - **公司**: %s
                - **职位**: %s
                - **候选人简历**:
                ---
                %s
                ---
                - **面试配置**:
                ---
                %s
                ---
                
                # 核心原则与约束
                1. **记忆很重要**: 总是回顾对话历史。**绝不**重复之前已经问过的问题。
                2. **保持流程**: 严格按照下面的“面试流程”执行。不要随机跳转到不同阶段。
                3. **不问非法问题**: 不得询问年龄、婚姻状况、国籍或其他受保护的个人信息。
                4. **不做最终决定**: 不要给出最终录用建议（例如：“你被录用了”）。你的角色是评估和反馈，而不是做决定。
                5. **保持公正**: 给予候选人解释自己的机会。如果他们紧张，给予鼓励。
                
                # 面试流程（阶段划分）
                请按照以下顺序引导面试流程：
                1.  `RESUME_DEEP_DIVE`: 这是核心阶段。对简历中的项目和技能提出详细问题。在此阶段花费最多时间。
                2.  `TECHNICAL_QUESTIONS`: 如果配置中有要求或简历深度不够，则提出有针对性的技术或行为问题。
                3.  `CANDIDATE_QUESTIONS`: 询问候选人是否有任何问题想问你。
                4.  `CLOSING`: 感谢候选人，简要总结对话内容，并说明下一步流程。
                
                # 回答方式要求
                - 请直接输出你要说给候选人听的自然语言句子，不要用 JSON 或其他结构。
                - 不要添加任何技术性描述、思考过程、策略标签等内部信息。
                - 所有回答都必须口语化、简洁、清晰，便于直接转成语音播报。
                """;

        String fullPrompt = String.format(systemContent,
                channel.getInterviewerStyle(),
                Optional.ofNullable(channel.getTargetCompany()).orElse("一家领先的科技公司"),
                Optional.ofNullable(channel.getTargetPosition()).orElse("一个技术岗位"),
                formattedResume,
                formattedChannel);

        return chatClient.prompt()
                .system(fullPrompt)
                .user(message)
                .advisors(spec -> spec
                        .advisors(
                                // 开启mysql
                                new MessageChatMemoryAdvisor(mysqlInterviewMemory),
                                ragCloudAdvisor  // 知识库顾问
                        )
                        .params(Map.of(
                                "chat_memory.conversation_id", chatId,
                                "chat_memory.retrieve_size", 10
                        ))
                )
                .stream()
                .content();
    }

    // 在面试结束后，根据分析过程，给出分析报告
    public AnalysisReportDTO aiInterviewWithAnalysisReport(String finalPrompt) {
        log.info("面试分析输入给ai的最终提示词：{}", finalPrompt);
        AnalysisReportDTO reportDTO = null;
        try {
            reportDTO = this.chatClient.prompt()
                    .user(finalPrompt)
                    .call()
                    .entity(AnalysisReportDTO.class);

            // 步骤 4: 使用ObjectMapper手动解析清理后的纯净JSON
        } catch (Exception e) {
            log.error("调用AI生成报告失败，sessionId: 错误: ,{}", e.getMessage());
        }
        log.info("成功从AI获取到会话的结构化报告{}", reportDTO);
        return reportDTO;
    }

    /**
     * 从一个可能包含前后缀文本的字符串中，稳健地提取出第一个完整的JSON对象。
     *
     * @param text AI返回的原始文本
     * @return 纯净的JSON字符串，如果找不到则返回空字符串。
     */
    private String extractJsonFromString(String text) {
        // 寻找第一个 '{' 和最后一个 '}'
        int firstBrace = text.indexOf('{');
        int lastBrace = text.lastIndexOf('}');

        if (firstBrace != -1 && lastBrace > firstBrace) {
            // 截取从第一个 '{' 到最后一个 '}' 的所有内容
            return text.substring(firstBrace, lastBrace + 1);
        }

        log.warn("在文本中未找到有效的JSON对象括号: {}", text);
        return ""; // 返回空字符串表示未找到
    }

    public TurnAnalysisResponse aiInterviewWithDialogue(String analysisPrompt) {
        return this.chatClient
                .prompt()
                .user(analysisPrompt)
                .call()
                .entity(TurnAnalysisResponse.class);
    }

    /**
     * 一个简单的工具类，用于从可能包含额外文本的字符串中提取出JSON部分。
     */
    class JsonExtractor {
        public static String extractJson(String text) {
            if (text == null || text.trim().isEmpty()) {
                return null;
            }
            int firstBrace = text.indexOf('{');
            int lastBrace = text.lastIndexOf('}');
            if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
                return text.substring(firstBrace, lastBrace + 1);
            }
            return null;
        }
    }

    /**
     * 专为实时视频+图片分析设计的提示词。
     * 增加了更多维度的输入数据和更详细的数据释义，以提升AI分析的准确性。
     */
    private static final String REALTIME_VIDEO_PICTURE_ANALYSIS_PROMPT =
            """
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
                    输出的JSON格式严格如下：
                    ```json
                    {
                      "summary": "一句话总结候选人当前的状态，用于实时显示给用户。",
                      "suggestion": "如果状态不佳，提供一句具体的、可操作的改进建议。如果状态良好，此字段为空字符串。",
                      "status": "状态标识符，只能是 'POSITIVE' 或 'NEEDS_IMPROVEMENT' 中的一个。",
                      "detailedAnalysis": "字符串，其中包含对眼神和情绪两方面表现的详细文字分析过程和结果。"
                    }
                    ```
                    请严格按照指定的JSON格式输出，以便系统存储分析结果。
                    """;
}
