package com.echo.virtual_interview.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.echo.virtual_interview.controller.ai.InterviewExpert;
import com.echo.virtual_interview.exception.BusinessException;
import com.echo.virtual_interview.mapper.*;
import com.echo.virtual_interview.model.dto.analysis.*;
import com.echo.virtual_interview.model.entity.*;
import com.echo.virtual_interview.service.IAnalysisReportsService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.formula.functions.T;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 存储详细的面试后分析报告 服务实现类
 * </p>
 *
 * @author echo
 * @since 2025-06-20
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AnalysisReportsServiceImpl extends ServiceImpl<AnalysisReportsMapper, AnalysisReports> implements IAnalysisReportsService {
    // 注入所有需要的Mapper
    private final AnalysisReportsMapper analysisReportMapper;
    private final InterviewSessionsMapper interviewSessionMapper;
    private final InterviewChannelsMapper interviewChannelMapper;
    private final InterviewDialogueMapper interviewDialogueMapper;
    private final LearningResourcesMapper learningResourceMapper;
    private final LearningResourcesTopicsMapper learningResourcesTopicMapper; // 使用更新后的Mapper
    private final InterviewLearningRecommendationsMapper recommendationMapper; // 用于写入推荐结果

    // 注入AI客户端和JSON处理工具
    private final InterviewExpert interviewExpert;
    private final ObjectMapper objectMapper;


    @Override
    @Cacheable(cacheNames = "interviewReports", key = "#sessionId")
    public InterviewReportResponseDTO getFullReportBySessionId(String sessionId) {
        // --- 步骤 1: 从数据库并行获取所有需要的数据 ---
        log.info("缓存未命中，开始执行 getFullReportBySessionId 逻辑，sessionId: {}", sessionId);
        log.info("开始为会话ID {} 生成报告", sessionId);
        AnalysisReports report = analysisReportMapper.selectOne(
                new LambdaQueryWrapper<AnalysisReports>().eq(AnalysisReports::getSessionId, sessionId)
        );
        InterviewSessions session = interviewSessionMapper.selectById(sessionId);

        // 数据校验
        if (report == null || session == null) {
            log.error("未找到ID为 {} 的面试报告或会话记录", sessionId);
            throw new RuntimeException("报告或会话记录不存在");
        }

        // 明确地通过 session.channel_id 获取 channel 信息
        InterviewChannels channel = interviewChannelMapper.selectById(session.getChannelId());
        if (channel == null) {
            log.error("未找到会话 {} 关联的频道ID {}", sessionId, session.getChannelId());
            throw new RuntimeException("面试频道信息不存在");
        }

        List<InterviewDialogue> dialogues = interviewDialogueMapper.selectList(
                new LambdaQueryWrapper<InterviewDialogue>()
                        .eq(InterviewDialogue::getSessionId, sessionId)
                        .orderByAsc(InterviewDialogue::getSequence)

        );
        // 此处省略获取简历信息的代码，实际中应一并获取

        // --- 步骤 2: 直接从数据库数据构建 question_analysis_data ---
        List<QuestionAnalysisDTO> questionAnalysisData = buildQuestionAnalysisFromDB(dialogues);

        // --- 步骤 3: 准备调用AI所需的数据和Prompt ---
        String aiMessage = buildAiMessage(channel, report, dialogues);
        String aiPrompt = buildAiPrompt();

        // --- 步骤 4: 调用AI服务获取高阶分析结果 ---
        log.info("为会话 {} 调用AI进行高阶分析", sessionId);
        String aiResultJson = interviewExpert.aiInterviewByAnalysis(aiPrompt, aiMessage);
        log.info("分析报告中ai的返回：{}", aiResultJson);
        String aiResultString = parseAiResult(aiResultJson);
        AiAnalysisResultDTO aiResult =null;
        try {
            aiResult=objectMapper.readValue(aiResultString, AiAnalysisResultDTO.class);
        }catch (JsonProcessingException e) {
            log.error("AI生成学习报告时，返回的JSON解析失败: {}", aiResultJson, e);
            throw new BusinessException(500,"AI生成学习报告失败");
        }

        // --- 步骤 5: 根据AI识别的薄弱知识点，查询并生成学习推荐 ---
        List<RecommendationDTO> recommendations = generateRecommendations(aiResult.getWeak_topics_with_reasons(), sessionId);

        // --- 步骤 6: 组装最终的响应DTO ---
        log.info("为会话 {} 组装最终报告", sessionId);
        return buildResponseDTO(report, session, channel, dialogues, aiResult, recommendations, questionAnalysisData);
    }

    /**
     * 新增方法：直接从数据库对话记录构建前端所需的“逐题分析”部分
     *
     * @param dialogues 从数据库查询出的对话列表
     * @return 组装好的QuestionAnalysisDTO列表
     */
    private List<QuestionAnalysisDTO> buildQuestionAnalysisFromDB(List<InterviewDialogue> dialogues) {
        if (dialogues == null || dialogues.isEmpty()) {
            return Collections.emptyList();
        }
        return dialogues.stream().map(dialogue -> {
            QuestionAnalysisDTO dto = new QuestionAnalysisDTO();
            dto.setQuestion(dialogue.getAiMessage());
            dto.setAnswer(dialogue.getUserMessage());
            dto.setScore(dialogue.getTurnScore());
            // 直接使用数据库中的 turn_suggestion 字段
            dto.setSuggestion(dialogue.getTurnSuggestion());
            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * 构建发送给AI的上下文信息 (Message)，此版本经过优化，仅包含对话的关键分析信息。
     *
     * @param channel   面试频道信息
     * @param report    分析报告的基础数据
     * @param dialogues 对话列表
     * @return 序列化后的JSON字符串
     */
    @SneakyThrows
    private String buildAiMessage(InterviewChannels channel, AnalysisReports report, List<InterviewDialogue> dialogues) {
        // 构建一个Map，用于序列化为JSON
        Map<String, Object> messageMap = new HashMap<>();

        // 1. 面试上下文
        Map<String, String> context = new HashMap<>();
        context.put("targetPosition", channel.getTargetPosition());
        context.put("jobType", channel.getJobType());
        messageMap.put("interviewContext", context);

        // 2. 性能评分
        Map<String, Object> scores = new HashMap<>();
        scores.put("professional_knowledge", report.getScoreProfessionalKnowledge());
        scores.put("skill_match", report.getScoreSkillMatch());
        scores.put("verbal_expression", report.getScoreVerbalExpression());
        scores.put("logical_thinking", report.getScoreLogicalThinking());
        messageMap.put("performanceScores", scores);

        // 3. 优化的对话摘要 (只包含分析和建议)
        // 这样做可以显著减少发送给AI的token数量，降低成本并聚焦任务
        List<Map<String, String>> dialogueSummaries = dialogues.stream().map(d -> {
            Map<String, String> summary = new LinkedHashMap<>();
            summary.put("analysis", d.getTurnAnalysis());
            summary.put("suggestion", d.getTurnSuggestion());
            return summary;
        }).collect(Collectors.toList());
        messageMap.put("dialogueSummaries", dialogueSummaries);

        // 4. 行为数据
        Map<String, Object> behavioralData = new HashMap<>();
        behavioralData.put("filler_word_usage", objectMapper.readValue(report.getFillerWordUsage(), new TypeReference<Map<String, Integer>>() {
        }));
        messageMap.put("behavioralData", behavioralData);

        // 5. 简历信息（此处省略）
        messageMap.put("resumeInfo", new HashMap<>());

        return objectMapper.writeValueAsString(messageMap);
    }


    /**
     * 构建一个用于生成面试综合分析报告的、经过优化的AI Prompt。
     * 这个版本的Prompt在输出格式要求上做了极大的强化，并加入了一个示例（One-shot），
     * 以确保AI返回纯净、可直接解析的JSON。
     *
     * @return 优化后的Prompt字符串。
     */
    private String buildAiPrompt() {
        return """
            # 角色
            你是一位资深的IT技术面试官和职业发展导师，你的任务是根据我提供的面试数据，生成一份结构化的JSON分析报告。

            # 任务
            基于我提供的JSON格式的面试综合数据（`message`），进行全面、深入的整合与升华分析。

            # 输入数据
            我将通过 `message` 字段提供一个JSON对象，其中包含：
            - `interviewContext`: 面试上下文，如应聘岗位。
            - `performanceScores`: 各项能力维度的评分。
            - `dialogueSummaries`: 一个列表，每项包含对一轮对话的`analysis`（分析）和`suggestion`（建议）。
            - `behavioralData`: 行为数据，如口头禅使用频率。
            - `resumeInfo`: 候选人简历的关键信息。

            # 分析内容要求
            1.  **总体评价 (overall_summary)**: 综合所有信息，给出一个客观、中肯的总体评价（约100字）。
            2.  **行为表现分析 (behavioral_analysis)**: 结合口头禅、眼神交流等数据，分析候选人的面试行为举止和沟通风格。
            3.  **行为改进建议 (behavioral_suggestions)**: 针对行为表现，提出2-3条具体、可行的改进建议。每条建议必须用分号`;`分隔。
            4.  **综合提升建议 (overall_suggestions)**: 结合候选人的技术表现和岗位要求，提出3-4条综合性的能力提升建议。每条建议必须用分号`;`分隔。
            5.  **识别薄弱知识点及推荐理由 (weak_topics_with_reasons)**:
                - 根据候选人的回答、得分和初步分析，识别出3个最需要加强的技术知识点。
                - 为每个知识点生成一段有针对性的“推荐理由 (`recommendation_reason`)”。
            6.  **能力雷达图评分 (radar_scores)**: 直接根据输入数据中的 `performanceScores`，返回一个包含六个维度得分的JSON对象。键名必须与示例中的完全一致
                - 其中professional_knowledge为专业知识，skill_match为技能匹配、verbal_expression为语音表达、logical_thinking为逻辑思维、innovation为创新能力、stress_resistance为应变抗压的打分，且不允许为0.00

            # 重要：输出格式要求 (!!! 必须严格遵守 !!!)
            你的整个响应**必须**是一个单一、完整且语法正确的JSON对象，不包含任何其他文本、解释或Markdown标记。

            ---
            # 示例
            ## 输入 (Message):
            {
              "performanceScores": { "professional_knowledge": 65, "skill_match": 70, "verbal_expression": 80, "logical_thinking": 75, "innovation": 60, "stress_resistance": 85 },
              "dialogueSummaries": [
                { "analysis": "对Redis持久化机制不了解，回答过于简单。", "suggestion": "建议系统学习Redis核心知识，特别是持久化部分。" }
              ]
            }

            ## 输出 (你的响应):
            {
              "overall_summary": "候选人对基础知识掌握不牢固，尤其在数据库方面表现出明显短板。沟通意愿尚可，但技术深度有待加强。",
              "behavioral_analysis": "在面对不熟悉的问题时，候选人选择直接回答不清楚，态度诚实但略显消极。",
              "behavioral_suggestions": "回答问题时可以尝试表达自己的思考过程，即使不确定答案;在面试前加强对岗位要求技术的复习。",
              "overall_suggestions": "系统性地学习后端核心技术;多参与项目实践以积累经验;模拟面试以提升表达能力。",
              "radar_scores": {
                  "professional_knowledge": 65,
                  "skill_match": 70,
                  "verbal_expression": 80,
                  "logical_thinking": 75,
                  "innovation": 60,
                  "stress_resistance": 85
              },
              "weak_topics_with_reasons": [
                {
                  "topic": "Redis持久化机制",
                  "recommendation_reason": "候选人对Redis的持久化机制完全不了解，这是后端开发中非常核心的知识点，需要立即补充学习。"
                }
              ]
            }
            ---

            # 正式任务
            现在，请根据我接下来在 `message` 中提供的真实数据，生成分析报告。

            ## 输出 (你的响应):
            """;
    }

    @SneakyThrows
    private String parseAiResult(String jsonContent) {
        // 第一道防线：检查传入的参数是否为null或空
        if (jsonContent == null || jsonContent.trim().isEmpty()) {
            log.error("AI未能返回任何有效内容，传入的字符串为null或空。");
            // 你可以返回一个默认的错误DTO，或者直接抛出异常
            throw new RuntimeException("AI未能生成分析内容，无法完成报告。");
        }

        // 第二道防线：尝试从文本中提取JSON
        // 这能处理AI返回了 "好的，这是JSON：{...}" 这种情况
        if (!jsonContent.trim().startsWith("{")) {
            int firstBrace = jsonContent.indexOf('{');
            int lastBrace = jsonContent.lastIndexOf('}');
            if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
                log.warn("AI返回的内容包含额外文本，正在尝试提取JSON部分...");
                jsonContent = jsonContent.substring(firstBrace, lastBrace + 1);
            }
        }
        return jsonContent;

    }
    @SneakyThrows
    private String parseAiResultWithArray(String jsonContent) {
        // 第一道防线：检查传入的参数是否为null或空
        if (jsonContent == null || jsonContent.trim().isEmpty()) {
            log.error("AI未能返回任何有效内容，传入的字符串为null或空。");
            // 你可以返回一个默认的错误DTO，或者直接抛出异常
            throw new RuntimeException("AI未能生成分析内容，无法完成报告。");
        }

        // 第二道防线：尝试从文本中提取JSON
        // 这能处理AI返回了 "好的，这是JSON：{...}" 这种情况
        if (!jsonContent.trim().startsWith("[")) {
            int firstBrace = jsonContent.indexOf('[');
            int lastBrace = jsonContent.lastIndexOf(']');
            if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
                log.warn("AI返回的内容包含额外文本，正在尝试提取JSON部分...");
                jsonContent = jsonContent.substring(firstBrace, lastBrace + 1);
            }
        }
        return jsonContent;

    }
    /**
     * 根据AI识别出的薄弱知识点，生成学习推荐列表。
     * 此方法会优先查询本地数据库，如果资源不足，则会调用AI生成新的资源并存入数据库。
     *
     * @param weakTopics AI分析出的薄弱知识点列表。
     * @param sessionId  当前面试的会话ID。
     * @return 学习推荐DTO列表。
     */
    private List<RecommendationDTO> generateRecommendations(List<AiAnalysisResultDTO.WeakTopicWithReason> weakTopics, String sessionId) {
        if (weakTopics == null || weakTopics.isEmpty()) {
            return Collections.emptyList();
        }

        List<RecommendationDTO> finalRecommendations = new ArrayList<>();
        List<AiAnalysisResultDTO.WeakTopicWithReason> topicsToGenerate = new ArrayList<>();

        // 步骤 1: 遍历所有薄弱点，优先从本地数据库查找
        for (AiAnalysisResultDTO.WeakTopicWithReason weakTopic : weakTopics) {
            List<LearningResourcesTopics> foundTopics = learningResourcesTopicMapper.selectList(
                    new LambdaQueryWrapper<LearningResourcesTopics>().like(LearningResourcesTopics::getName, weakTopic.getTopic())
            );

            if (!foundTopics.isEmpty()) {
                // 本地找到了，直接使用第一个匹配项
                Integer topicId = foundTopics.get(0).getId();
                LearningResources resource = learningResourceMapper.selectOne(
                        new LambdaQueryWrapper<LearningResources>().eq(LearningResources::getTopicId, topicId).last("LIMIT 1")
                );
                if (resource != null) {
                    RecommendationDTO dto = new RecommendationDTO();
                    dto.setId(resource.getId());
                    dto.setTitle(resource.getTitle());
                    dto.setUrl(resource.getUrl());
                    dto.setRecommendation_reason(weakTopic.getRecommendation_reason());
                    finalRecommendations.add(dto);
                }
            } else {
                // 本地未找到，将此薄弱点加入待生成列表
                topicsToGenerate.add(weakTopic);
            }
        }

        // 步骤 2: 如果有需要生成的知识点，则批量调用AI
        if (!topicsToGenerate.isEmpty()) {
            log.info("本地资源不足，准备为 {} 个知识点调用AI生成新资源。", topicsToGenerate.size());
            List<RecommendationDTO> generatedRecommendations = generateAndSaveNewResourcesViaAI(topicsToGenerate, sessionId);
            finalRecommendations.addAll(generatedRecommendations);
        }

        // 步骤 3: 为最终的推荐列表设置step_number
        int step = 1;
        for (RecommendationDTO dto : finalRecommendations) {
            dto.setStep_number(step++);
        }

        return finalRecommendations;
    }

    /**
     * 调用AI生成新的知识点和学习资源，并将其存入数据库。
     *
     * @param topicsToGenerate 需要生成资源的薄弱知识点列表。
     * @param sessionId        当前会话ID，用于记录。
     * @return 为新生成的资源创建的推荐DTO列表。
     */
    private List<RecommendationDTO> generateAndSaveNewResourcesViaAI(List<AiAnalysisResultDTO.WeakTopicWithReason> topicsToGenerate, String sessionId) {
        // 1. 构建Prompt和Message
        String prompt = buildResourceGenerationPrompt();
        String message = buildResourceGenerationMessage(topicsToGenerate.stream().map(AiAnalysisResultDTO.WeakTopicWithReason::getTopic).collect(Collectors.toList()));

        // 2. 调用AI服务
        String aiResultJson = interviewExpert.aiInterviewByrecommendations(prompt, message);
        String aiResultString = parseAiResultWithArray(aiResultJson);

        // 3. 解析AI返回的JSON
        List<RecommendationDTO> newRecommendations = new ArrayList<>();
        try {
            List<AiGeneratedResourceDTO> generatedResources = objectMapper.readValue(aiResultString, new TypeReference<>() {
            });

            // 4. 遍历并存入数据库
            for (AiGeneratedResourceDTO generated : generatedResources) {
                // 4.1 保存Topic
                LearningResourcesTopics newTopic = new LearningResourcesTopics();
                newTopic.setName(generated.getTopic().getName());
                newTopic.setDescription(generated.getTopic().getDescription());
                newTopic.setCategory(generated.getTopic().getCategory());
                learningResourcesTopicMapper.insert(newTopic); // 插入后，ID会自动回填到newTopic对象中

                // 4.2 保存Resources
                // 4.2 遍历并保存AI返回的所有资源
                if (generated.getResources() != null && !generated.getResources().isEmpty()) {
                    for (AiGeneratedResourceDTO.ResourceDetail resourceDetail : generated.getResources()) {
                        LearningResources newResource = new LearningResources();
                        newResource.setTopicId(newTopic.getId()); // 使用新生成的Topic ID
                        newResource.setTitle(resourceDetail.getTitle());
                        newResource.setDescription(resourceDetail.getDescription());
                        newResource.setUrl(resourceDetail.getUrl());

                        // 【重要改动】直接为String类型的字段赋值，不再进行Enum转换
                        newResource.setResourceType(resourceDetail.getResource_type());
                        newResource.setDifficulty(resourceDetail.getDifficulty());

                        learningResourceMapper.insert(newResource);

                        // 4.3 为每一个保存的资源创建一个DTO用于返回
                        RecommendationDTO dto = new RecommendationDTO();
                        dto.setId(newResource.getId());
                        dto.setTitle(newResource.getTitle());
                        dto.setUrl(newResource.getUrl());
                        // 找到原始的推荐理由
                        topicsToGenerate.stream()
                                .filter(t -> t.getTopic().equals(newTopic.getName()))
                                .findFirst()
                                .ifPresent(t -> dto.setRecommendation_reason(t.getRecommendation_reason()));
                        newRecommendations.add(dto);
                    }
                }
            }
        } catch (JsonProcessingException e) {
            log.error("AI生成学习资源时，返回的JSON解析失败: {}", aiResultJson, e);
            // 解析失败，返回空列表，不影响主流程
            return Collections.emptyList();
        }
        return newRecommendations;
    }

    /**
     * 为AI资源生成任务构建专用的Prompt。
     *
     * @return Prompt字符串。
     */
    private String buildResourceGenerationPrompt() {
        return """
                # 角色
                你是一位顶级的教育内容策划专家和各领域技术专家。
                
                # 任务
                根据我提供的一系列技术或技能主题词（这些是用户表现薄弱的知识点），为每一个主题词生成一个相关的知识点定义和至少一个高质量的学习资源。
                
                # 输入
                我将提供一个JSON数组，其中包含需要生成内容的主题词名称。
                
                # 输出格式要求 (!!! 必须严格遵守 !!!)
                你的响应必须是一个纯净、合法的JSON数组。数组中的每个对象代表一个我输入的主题词，且必须严格遵循以下结构：
                ```json
                [
                  {
                    "topic": {
                      "name": "主题词名称 (与输入完全一致)",
                      "description": "对该知识点的简洁、准确的描述 (约50-100字)",
                      "category": "知识点分类 (例如: 后端技术, 前端技术, 数据库, 软技能, 计算机基础等)"
                    },
                    "resources": [
                      {
                        "title": "推荐的学习资源标题",
                        "description": "对该资源的简要介绍，说明其价值",
                        "url": "一个真实有效的URL链接",
                        "resource_type": "资源类型，只能是 'ARTICLE', 'VIDEO', 'COURSE', 'BOOK' 中的一个",
                        "difficulty": "难度等级，只能是 'BEGINNER', 'INTERMEDIATE', 'ADVANCED' 中的一个"
                      }
                    ]
                  }
                ]
                ```
                - **禁止**在JSON前后添加任何额外文本。
                - **禁止**使用Markdown。
                - 确保所有字段都存在且值类型正确。
                """;
    }

    /**
     * 为AI资源生成任务构建Message。
     *
     * @param topicNames 需要生成资源的主题词列表。
     * @return 序列化后的JSON字符串。
     */
    @SneakyThrows
    private String buildResourceGenerationMessage(List<String> topicNames) {
        return objectMapper.writeValueAsString(topicNames);
    }


    /**
     * 组装最终返回给前端的DTO对象
     * @param report 报告实体
     * @param session 会话实体
     * @param channel 频道实体
     * @param dialogues 对话列表
     * @param aiResult AI分析结果
     * @param recommendations 推荐列表
     * @param questionAnalysisData 问题分析列表
     * @return 完整的报告响应DTO
     */
    @SneakyThrows
    private InterviewReportResponseDTO buildResponseDTO(
            AnalysisReports report, InterviewSessions session, InterviewChannels channel,
            List<InterviewDialogue> dialogues, AiAnalysisResultDTO aiResult,
            List<RecommendationDTO> recommendations, List<QuestionAnalysisDTO> questionAnalysisData) {

        InterviewReportResponseDTO response = new InterviewReportResponseDTO();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        // 基本信息
        response.setPosition(channel.getTargetPosition());
        response.setInterviewType(channel.getJobType() + " - 技术面");
        response.setInterviewDate(session.getStartedAt().format(formatter));
        response.setGenerated_at(report.getGeneratedAt().format(formatter));

        // AI高阶分析结果
        response.setOverall_summary(aiResult.getOverall_summary());
        response.setBehavioral_analysis(aiResult.getBehavioral_analysis());
        response.setBehavioral_suggestions(aiResult.getBehavioral_suggestions());
        response.setOverall_suggestions(aiResult.getOverall_suggestions());

        // 学习推荐 (来自AI分析+DB查询)
        response.setRecommendations(recommendations);

        // 逐题分析 (直接来自DB)
        response.setQuestion_analysis_data(questionAnalysisData);

        // 对话记录 (直接来自DB)
        response.setDialogues(dialogues.stream().map(d -> {
            DialogueDTO dto = new DialogueDTO();
            dto.setId(d.getId());
            dto.setSequence(d.getSequence());
            dto.setAi_message(d.getAiMessage());
            dto.setUser_message(d.getUserMessage());
            dto.setTimestamp(d.getTimestamp().toString());
            dto.setTurn_analysis(d.getTurnAnalysis());
            return dto;
        }).collect(Collectors.toList()));

        // 【重要改动】雷达图数据现在直接从AI的返回结果中获取
        RadarDataDTO radarData = new RadarDataDTO();
        radarData.setIndicator(Arrays.asList(
                new RadarDataDTO.Indicator("专业知识"), new RadarDataDTO.Indicator("技能匹配"),
                new RadarDataDTO.Indicator("语言表达"), new RadarDataDTO.Indicator("逻辑思维"),
                new RadarDataDTO.Indicator("创新能力"), new RadarDataDTO.Indicator("应变抗压")
        ));

        RadarDataDTO.SeriesData seriesData = new RadarDataDTO.SeriesData();
        // 从aiResult中获取分数
        AiAnalysisResultDTO.RadarScores scores = aiResult.getRadar_scores();
        log.info("ai分析报告中返回的雷达图数据：{}", scores);
        if (scores != null) {
            seriesData.setValue(Arrays.asList(
                    scores.getProfessional_knowledge(), scores.getSkill_match(),
                    scores.getVerbal_expression(), scores.getLogical_thinking(),
                    scores.getInnovation(), scores.getStress_resistance()
            ));
        } else {
            // 如果AI由于某种原因没有返回分数，提供一个默认值或空值，防止空指针
            seriesData.setValue(Arrays.asList(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        }
        radarData.setSeries(Collections.singletonList(seriesData));
        response.setRadar_data(radarData);

        // 各项图表数据
        response.setTension_curve_data(parseJsonToChartData(report.getTensionCurveData(), "time", "value"));
        response.setFiller_word_usage(parseJsonToChartData(report.getFillerWordUsage()));
        response.setEye_contact_percentage(parseJsonToChartData(report.getEyeContactPercentage()));

        return response;
    }

        // 工具方法：将数据库中的JSON字符串解析为前端需要的图表格式
        @SneakyThrows
        private ChartDataDTO parseJsonToChartData(String json, String keyField, String valueField) {
            ChartDataDTO chartData = new ChartDataDTO();
            List<Map<String, Object>> dataList = objectMapper.readValue(json, new TypeReference<>() {
            });
            chartData.setXAxisData(dataList.stream().map(m -> m.get(keyField).toString()).collect(Collectors.toList()));
            chartData.setSeriesData(dataList.stream().map(m -> m.get(valueField)).collect(Collectors.toList()));
            return chartData;
        }

        // 重载方法处理口头禅和眼神交流这种key-value格式的JSON
        @SneakyThrows
        private ChartDataDTO parseJsonToChartData(String json) {
            ChartDataDTO chartData = new ChartDataDTO();
            Map<String, Object> dataMap = objectMapper.readValue(json, new TypeReference<>() {
            });
            chartData.setXAxisData(new ArrayList<>(dataMap.keySet()));
            chartData.setSeriesData(new ArrayList<>(dataMap.values()));
            return chartData;
        }
    }

