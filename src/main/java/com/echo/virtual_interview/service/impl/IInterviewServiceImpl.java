package com.echo.virtual_interview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.echo.virtual_interview.common.ErrorCode;
import com.echo.virtual_interview.context.UserIdContext;
import com.echo.virtual_interview.controller.ai.InterviewExpert;
import com.echo.virtual_interview.exception.BusinessException;
import com.echo.virtual_interview.mapper.*;
import com.echo.virtual_interview.model.dto.experience.InterviewHistoryDTO;
import com.echo.virtual_interview.model.dto.history.InterviewHistoryCardDTO;
import com.echo.virtual_interview.model.dto.interview.AnalysisReportDTO;
import com.echo.virtual_interview.model.dto.interview.TurnAnalysisResponse;
import com.echo.virtual_interview.model.dto.interview.audio.AudioResponseDto;
import com.echo.virtual_interview.model.dto.interview.channel.ChannelDetailDTO;
import com.echo.virtual_interview.model.dto.interview.process.IflytekExpressionResponse;
import com.echo.virtual_interview.model.dto.interview.process.RealtimeFeedbackDto;
import com.echo.virtual_interview.model.dto.interview.process.VideoAnalysisPayload;
import com.echo.virtual_interview.model.dto.resum.ResumeDataDto;
import com.echo.virtual_interview.model.entity.*;
import com.echo.virtual_interview.service.*;
import com.echo.virtual_interview.utils.face.IflytekExpressionService;
import com.echo.virtual_interview.utils.tst.TtsService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class IInterviewServiceImpl implements IInterviewService {

    @Resource
    private IResumeService resumeService;
    @Resource
    private IInterviewSessionsService sessionsService;
    @Resource
    private IInterviewChannelsService channelsService;
    @Resource
    private IResumeModuleService resumeModuleService;

    @Resource
    private InterviewSessionsMapper interviewSessionsMapper;

    @Resource
    private ObjectMapper objectMapper;
    @Resource
    private InterviewAnalysisChunkMapper analysisChunkMapper;

    @Resource
    private InterviewExpert interviewExpert;
    @Resource
    private InterviewDialogueMapper dialogueMapper;

    @Resource
    private IflytekExpressionService iflytekExpressionService; // 注入表情分析的讯飞服务
    @Resource
    private InterviewSessionsMapper sessionsMapper;
    @Resource
    private IInterviewAnalysisChunkService chunkService;
    @Resource
    private InterviewAnalysisChunkMapper chunkMapper;
    @Resource
    private AnalysisReportsMapper reportsMapper;
    @Resource
    private TtsService ttsService;
    @Resource
    private SimpMessagingTemplate messagingTemplate; // 2. 注入 SimpMessagingTemplate

    /**
     * 【核心重构】面试核心业务流程。
     * 此方法现在只负责定义一个完整的、从“接收用户消息”到“返回AI回复流”的响应式工作流。
     * 它不再直接执行任何操作，而是返回一个包含所有业务逻辑的 Flux，由调用者（AsrProcessingService）来订阅并触发执行。
     *
     * @param message   用户的完整回答文本。
     * @param sessionId 当前会话ID。
     * @param userId    用户ID。
     * @return 一个包含AI流式回复的Flux<String>。
     */
    @Override
    public Flux<String> interviewProcess(String message, String sessionId, Integer userId) {
        // 【步骤A - 承上】: 更新上一轮对话的用户回答。
        // 使用 Mono.fromRunnable 将同步的数据库操作包装成一个响应式类型，并确保它在流的开始执行。
        Mono<InterviewDialogue> updatePreviousTurnMono = Mono.fromCallable(() -> {
            InterviewDialogue pendingDialogue = dialogueMapper.findLatestPendingDialogue(sessionId);
            if (pendingDialogue == null) {
                // 如果没有等待中的AI提问，这是一个严重的业务流程错误。
                log.error("严重错误：在会话 {} 中收到用户消息，但没有找到待处理的AI提问！", sessionId);
                throw new IllegalStateException("对话状态异常，无法处理您的消息。");
            }
            pendingDialogue.setUserMessage(message);
            dialogueMapper.updateById(pendingDialogue);
            log.info("已更新面试会话 {} 的第 {} 轮对话，用户回答已存入。", sessionId, pendingDialogue.getSequence());

            // 启动异步分析任务 (这个是旁路任务，不影响主流程)
            performTurnAnalysisAsync(pendingDialogue);

            return pendingDialogue;
        }).subscribeOn(Schedulers.boundedElastic()); // 【并发模型】将数据库操作切换到专用的工作线程池，避免阻塞调用者线程。

        // 【步骤B - 启下】: 请求AI生成新问题。
        // 定义一个获取AI回复流的Mono。使用defer使其懒加载，只有在被订阅时才执行。
        Mono<Flux<String>> getAiStreamMono = Mono.defer(() -> {
            log.info("用户 (sessionId: {}) 发送消息: {}，准备请求AI生成下一轮问题...", sessionId, message);
            // 获取上下文信息
            ResumeDataDto resume = resumeService.getResumeByUserId(userId);
            Long resumeId = resumeService.getResumeIdByUserId(userId);
            List<ResumeModule> resumeModules = resumeModuleService.getResumeModulesByResumId(resumeId);
            InterviewSessions session = sessionsService.getById(sessionId);
            ChannelDetailDTO channel = channelsService.getChannelDetailsNoAdd(session.getChannelId());

            // 调用AI专家获取流
            return Mono.just(interviewExpert.aiInterviewByStreamWithProcess(message, sessionId, resume, resumeModules, channel));
        });

        // 【关键修复】使用 .then() 和 .flatMapMany() 正确地将步骤A和步骤B串联起来。
        // 1. 首先执行 updatePreviousTurnMono (更新上一轮)
        // 2. .then(getAiStreamMono) 在上一步完成后，执行 getAiStreamMono (获取AI流)
        // 3. .flatMapMany(flux -> flux) 将 Mono<Flux<String>> 扁平化为 Flux<String>
        return updatePreviousTurnMono
                .then(getAiStreamMono)
                .flatMapMany(flux -> {
                    // 【设计模式改进】将AI回复的存储逻辑也移到这里，在流结束时执行。
                    StringBuilder aiResponseCollector = new StringBuilder();
                    return flux
                            .doOnNext(aiResponseCollector::append)
                            .doOnComplete(() -> {
                                String fullAiResponse = aiResponseCollector.toString().trim();
                                if (fullAiResponse.isEmpty()) {
                                    log.warn("AI为会话 {} 生成的响应为空，不创建新的对话轮次。", sessionId);
                                    return;
                                }
                                // 创建并插入代表新一轮AI提问的记录
                                Integer maxSequence = dialogueMapper.getMaxSequence(sessionId);
                                InterviewDialogue newDialogue = new InterviewDialogue();
                                newDialogue.setSessionId(sessionId);
                                newDialogue.setSequence(maxSequence == null ? 1 : maxSequence + 1);
                                newDialogue.setAiMessage(fullAiResponse);
                                newDialogue.setUserMessage(null);
                                newDialogue.setTimestamp(LocalDateTime.now());
                                try {
                                    dialogueMapper.insert(newDialogue);
                                    log.info("成功为会话 {} 创建了第 {} 轮新对话。", sessionId, newDialogue.getSequence());
                                } catch (Exception e) {
                                    log.error("为会话 {} 创建新对话失败！", sessionId, e);
                                }
                            });
                })
                // 【并发模型】同样，将整个AI请求和后续数据库操作都放在工作线程池上执行
                .subscribeOn(Schedulers.boundedElastic());
    }
    /**
     * 异步执行对单轮对话的AI分析，并将结果更新回数据库。
     * 使用 CompletableFuture.runAsync() 确保此操作在后台线程池中运行，不会阻塞主流程。
     *
     * @param completedDialogue 已经包含AI提问和用户回答的完整对话实体
     */
    /**
     * 异步执行对单轮对话的AI分析，并将结构化的结果（分数、建议、分析）更新回数据库。
     * 使用 CompletableFuture.runAsync() 确保此操作在后台线程池中运行，不会阻塞主流程。
     *
     * @param completedDialogue 已经包含AI提问和用户回答的完整对话实体
     */
    private void performTurnAnalysisAsync(final InterviewDialogue completedDialogue) {
        CompletableFuture.runAsync(() -> {
            try {
                log.info("启动对会话 {} 第 {} 轮的异步分析...", completedDialogue.getSessionId(), completedDialogue.getSequence());

                // 1. 构建分析专用的提示词 (新版prompt，要求返回JSON)
                String analysisPrompt = buildTurnAnalysisPrompt(
                        completedDialogue.getAiMessage(),
                        completedDialogue.getUserMessage()
                );

                // 2. 调用AI获取结构化的分析结果
                // 使用 .entity() 方法让 Spring AI 自动将返回的 JSON 转换为 TurnAnalysisResponse 对象
                TurnAnalysisResponse analysisResponse = interviewExpert.aiInterviewWithDialogue(analysisPrompt);

                // 3. 将分析结果的各个部分更新到数据库实体
                completedDialogue.setTurnScore(analysisResponse.turnScore());
                completedDialogue.setTurnSuggestion(analysisResponse.turnSuggestion());
                completedDialogue.setTurnAnalysis(analysisResponse.turnAnalysis());

                int updatedRows = dialogueMapper.updateById(completedDialogue);

                if (updatedRows > 0) {
                    log.info("成功完成并保存了会话 {} 第 {} 轮的结构化分析。", completedDialogue.getSessionId(), completedDialogue.getSequence());
                } else {
                    log.warn("更新会话 {} 第 {} 轮的分析结果失败，可能记录已被删除。", completedDialogue.getSessionId(), completedDialogue.getSequence());
                }

            } catch (Exception e) {
                // 异常可能包括AI调用失败、JSON解析失败等
                log.error("在对会话 {} 第 {} 轮进行异步分析时发生错误: {}", completedDialogue.getSessionId(), completedDialogue.getSequence(), e.getMessage(), e);
            }
        });
    }

    /**
     * 构建用于单轮对话分析的AI提示词
     *
     * @param question 面试官提出的问题
     * @param answer   候选人的回答
     * @return 构造好的完整提示词
     */
    /**
     * 构建用于单轮对话分析的AI提示词，要求AI以结构化的JSON格式返回结果。
     *
     * @param question 面试官提出的问题
     * @param answer   候选人的回答
     * @return 构造好的完整提示词
     */
    private String buildTurnAnalysisPrompt(String question, String answer) {
        return String.format("""
            # 角色
            你是一位专业的面试分析AI助手。

            # 核心任务
            根据面试官的问题和候选人的回答，进行客观、简洁、深入的分析。你的分析需要基于以下维度：
            - **回答相关性**: 回答是否直接命中了问题的核心？
            - **技术深度/逻辑性**: 回答是否展现了足够的技术深度（技术问题）或清晰的逻辑思维（行为问题）？
            - **结构与清晰度**: 回答的组织结构是否清晰，语言表达是否流畅？
            - **亮点与不足**: 回答中有哪些突出的亮点或明显的不足？

            # 分析对象
            - **面试官提问**: "%s"
            - **候选人回答**: "%s"

            # 在输出中，各名称代表的含义是：
              - **turnScore**：(int)范围1-100，综合评估候选人本轮表现的分数,
              - **turnSuggestion**: （String）针对候选人的回答，提出1-2点具体、可执行的改进建议。语言要精炼、有建设性。,
              - **turnAnalysis**: （String）一段综合分析文本，融合'核心任务'中提到的四个分析维度。先总结亮点，再指出不足。例如：候选人的回答展现了...，但在...方面稍有欠缺。
            """, question, answer);
    }

    // 开启面试
    @Override
    public String start(Long channelId) {
        Integer userId = UserIdContext.getUserIdContext();
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        // 获取简历ID（处理可能的空值）
        Long resumeId = Optional.ofNullable(resumeService.getResumeIdByUserId(userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.OPERATION_ERROR, "请先上传简历"));

        // 创建会话ID
        String sessionId = String.format("%d-%s-%d",
                userId, channelId, System.currentTimeMillis());
        // 更新面试次数
        InterviewChannels interviewChannels = interviewChannelMapper.selectById(channelId);
        int newCount = interviewChannels.getUsageCount() + 1;
        interviewChannels.setUsageCount(newCount);
        interviewChannelMapper.updateById(interviewChannels);
        // 保存会话记录
        InterviewSessions session = new InterviewSessions()
                .setId(sessionId)
                .setChannelId(channelId)
                .setUserId(userId.longValue())
                .setResumeId(resumeId)
                .setEndedAt(LocalDateTime.now())
                .setStartedAt(LocalDateTime.now())
                .setStatus("进行中");

        interviewSessionsMapper.insert(session);
        return sessionId;
    }

    /**
     * 实现了完整的处理流程：
     * 1. 调用讯飞API分析图片。
     * 2. 整合Python视频分析结果和图片分析结果。
     * 3. 调用大模型生成简短的实时反馈。
     * 4. 将所有分析数据持久化到数据库。
     *
     * @param payload 从Python服务接收到的完整数据。
     * @return 包含反馈信息的DTO对象，或在失败时返回null。
     */
    @Transactional
    public RealtimeFeedbackDto processAndStoreAnalysis(VideoAnalysisPayload payload) {
        // --- 1. 对两张关键帧图片进行表情分析 ---
        Optional<IflytekExpressionResponse> firstFrameAnalysisOpt = iflytekExpressionService.analyzeExpression(
                payload.getFirstFrameBase64(), "first_frame_" + payload.getAnalysisId() + ".jpg");
        Optional<IflytekExpressionResponse> lastFrameAnalysisOpt = iflytekExpressionService.analyzeExpression(
                payload.getLastFrameBase64(), "last_frame_" + payload.getAnalysisId() + ".jpg");

        // --- 2. 组装用于AI分析的输入数据 ---
        if (payload.getAnalysisByHalfMinute() == null || payload.getAnalysisByHalfMinute().isEmpty()) {
            log.warn("ID: {} 的分析数据中 analysisByHalfMinute 为空，无法进行AI分析。", payload.getAnalysisId());
            String summary = "请您调整坐姿，确保面部正对摄像头且光线充足，这样我们才能给您更准确的反馈。请继续加油！";
            String suggestion = "请您调整坐姿，确保面部正对摄像头且光线充足，这样我们才能给您更准确的反馈。请继续加油！";
            String status = "NEEDS_IMPROVEMENT"; // 提示用户需要做出调整
            String detailedAnalysis = "系统因未能从视频中提取有效的分析块(analysisByHalfMinute为空)。";
            return new RealtimeFeedbackDto(summary, suggestion, status, detailedAnalysis);
        }
        var latestReport = payload.getAnalysisByHalfMinute().get(payload.getAnalysisByHalfMinute().size() - 1);

        StringBuilder dataForAI = new StringBuilder();
        dataForAI.append(String.format("- **核心紧张度**: %s\n", latestReport.getNervousnessAnalysis().getLevel()));
        dataForAI.append(String.format("- **平均注意力分数**: %.2f\n", latestReport.getAttentionMean()));
        dataForAI.append(String.format("- **注意力稳定性**: %.3f\n", latestReport.getAttentionStability()));
        dataForAI.append(String.format("- **总眨眼次数**: %d\n", latestReport.getTotalBlinks()));
        dataForAI.append(String.format("- **头部晃动幅度**: %.2f\n", latestReport.getHeadPose().getYawFluctuation()));
        // 提前提取表情分析结果
        String firstFrameExpression = firstFrameAnalysisOpt
                .flatMap(res -> res.data().fileList().stream().findFirst())
                .map(file -> getExpressionFromLabel(file.label()))
                .orElse("未知");

        String lastFrameExpression = lastFrameAnalysisOpt
                .flatMap(res -> res.data().fileList().stream().findFirst())
                .map(file -> getExpressionFromLabel(file.label()))
                .orElse("未知");

        // 将结果添加到dataForAI中
        dataForAI.append(String.format("- **开场表情**: %s\n", firstFrameExpression));
        dataForAI.append(String.format("- **当前表情**: %s\n", lastFrameExpression));

        // --- 3. 调用AI大模型进行实时分析 ---
        RealtimeFeedbackDto realtimeFeedback = null;
        if (!dataForAI.isEmpty()) {
            log.info("--- 发送给AI的数据 ---\n{}", dataForAI.toString());
            realtimeFeedback = interviewExpert.aiInterviewByVideoAndPicture(dataForAI.toString());
            log.info("--- AI返回的结构化反馈 ---\n{}", realtimeFeedback);
        }

        // --- 4. 持久化所有分析结果到数据库 ---
        InterviewAnalysisChunk chunkEntity = new InterviewAnalysisChunk();
        try {
            // 4a. 设置基础信息
            chunkEntity.setId(payload.getAnalysisId()); // 使用Python传来的analysisId作为主键
            chunkEntity.setSessionId(payload.getSessionId());
//            chunkEntity.setDialogueId(null); // TODO: 需要有机制获取当前的对话ID
//            chunkEntity.setStartTimeSeconds(latestReport.getStartTimeSeconds());
//            chunkEntity.setEndTimeSeconds(latestReport.getEndTimeSeconds());

            // 4b. 存储Python的完整分析报告 (JSON字符串)
            chunkEntity.setPythonAnalysisReport(objectMapper.writeValueAsString(latestReport));

            // 4c. 存储讯飞的表情分析报告 (JSON字符串)
            Map<String, Object> expressionReport = new HashMap<>();
            expressionReport.put("表情1", firstFrameExpression);
            expressionReport.put("表情2", lastFrameExpression);
            chunkEntity.setIflytekExpressionReport(objectMapper.writeValueAsString(expressionReport));

            // 4d. 存储AI的完整反馈报告 (JSON字符串)
            if (realtimeFeedback != null) {
                chunkEntity.setAiFeedbackReport(objectMapper.writeValueAsString(realtimeFeedback));
            }

            // 4e. 保存到数据库 (MyBatis-Plus会自动处理createdAt和updatedAt)
            analysisChunkMapper.insert(chunkEntity);
            log.info("✅ 成功将分析块 {} 持久化到数据库。", chunkEntity.getId());

        } catch (Exception e) {
            log.error("持久化分析块 {} 失败。", payload.getAnalysisId(), e);
            throw new RuntimeException("持久化分析数据失败", e); // 抛出异常以触发事务回滚
        }

        return realtimeFeedback; // 最后返回给调用方的实时反馈DTO
    }


    @Override
    @Async("taskExecutor")
    public void end(Integer userId, String sessionId) {
        //结束面试时需要做的
        InterviewSessions interviewSessionsOld = sessionsMapper.selectById(sessionId);
        String status = interviewSessionsOld.getStatus();
        if (status.equals("已完成")) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "已经完成面试，无需再次分析");
        }
        //1.断开连接 【前端已自主完成】

        //2.完善视频每一轮对话的分析（现在得去找一些 哪里存储了interview_dialogue 这里应该得加逻辑去分析每一轮对话【在interviewProcess已经实现了】

        //3.完善视频流分析，analysis_chunk (属于那一轮对话的分析？）、完善interview_analysis_chunk的start_time_seconds、end_time_seconds、dialogue_id
        fitTable(sessionId);
        InterviewSessions session = sessionsMapper.selectById(sessionId);
        // ========================【新增】步骤 3.5: 预处理关键指标 ========================
        log.info("开始为会话 {} 预处理关键行为指标...", sessionId);
        // 重新获取数据，因为 fitTable() 可能已经更新了它们
        List<InterviewDialogue> dialogues = dialogueMapper.selectList(
                new QueryWrapper<InterviewDialogue>().eq("session_id", sessionId).orderByAsc("sequence")
        );
        List<InterviewAnalysisChunk> chunks = chunkMapper.selectList(
                new QueryWrapper<InterviewAnalysisChunk>().eq("session_id", sessionId).orderByAsc("start_time_seconds")
        );

        // 3.5.1 提取紧张度曲线数据
        List<Map<String, Object>> tensionCurve = extractTensionCurveData(chunks);

        // 3.5.2 提取口头禅使用频率
        Map<String, Integer> fillerWords = extractFillerWordUsage(dialogues);

        // 3.5.3 提取眼神交流数据
        Map<String, Double> eyeContact = extractEyeContactData(chunks, dialogues);
        // ==============================================================================


        // 步骤 4: 根据得到的所有分析结果，生成最终分析报告
        // 4.1 将预处理结果一同传入，生成最终的提示词
        String finalPrompt = generatePrompt(sessionId, tensionCurve, fillerWords, eyeContact);

        // 4.2 交给AI (这部分不变)
        AnalysisReportDTO reportDTO = interviewExpert.aiInterviewWithAnalysisReport(finalPrompt);

        // 步骤 5: 数据持久化 (这部分不变)
        // 注意：reportDTO 中由AI生成的字段（如 tension_curve_data）可能会覆盖我们的精确计算。
        // 在 saveReportData 中可以考虑用我们自己计算的值来强制覆盖AI的输出。
        saveReportData(session, reportDTO, tensionCurve, fillerWords, eyeContact);

    }

    /**
     * 从分析块中提取紧张度曲线数据。
     */
    private List<Map<String, Object>> extractTensionCurveData(List<InterviewAnalysisChunk> chunks) {
        List<Map<String, Object>> tensionData = new ArrayList<>();
        if (CollectionUtils.isEmpty(chunks)) return tensionData;

        for (InterviewAnalysisChunk chunk : chunks) {
            if (chunk.getPythonAnalysisReport() == null || chunk.getStartTimeSeconds() == null) continue;

            try {
                // 使用Jackson解析JSON
                JsonNode rootNode = objectMapper.readTree(chunk.getPythonAnalysisReport());
                JsonNode nervousnessNode = rootNode.path("nervousness_analysis");

                // 提取紧张度分数，如果不存在则跳过
                if (nervousnessNode.has("overall_score")) {
                    double score = nervousnessNode.get("overall_score").asDouble(0); // 默认为0
                    int timeSeconds = chunk.getStartTimeSeconds();
                    String timeFormatted = String.format("%02d:%02d", timeSeconds / 60, timeSeconds % 60);

                    tensionData.add(Map.of("time", timeFormatted, "value", score * 100)); // 假设分数是0-1，转为0-100
                }
            } catch (IOException e) {
                log.error("解析python_analysis_report失败, chunkId: {}", chunk.getId(), e);
            }
        }
        return tensionData;
    }

    /**
     * 从对话历史中提取口头禅使用频率。
     */
    private Map<String, Integer> extractFillerWordUsage(List<InterviewDialogue> dialogues) {
        Map<String, Integer> fillerWordCount = new HashMap<>();
        if (CollectionUtils.isEmpty(dialogues)) return fillerWordCount;

        // 定义要统计的口头禅列表
        String[] fillerWordsToTrack = {"嗯", "呃", "这个", "那个", "就是", "然后"};

        // 将所有用户回答拼接成一个长文本
        String allUserMessages = dialogues.stream()
                .map(InterviewDialogue::getUserMessage)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" "));

        if (allUserMessages.isEmpty()) return fillerWordCount;

        // 统计每个口头禅出现的次数
        for (String word : fillerWordsToTrack) {
            int count = (allUserMessages.length() - allUserMessages.replace(word, "").length()) / word.length();
            if (count > 0) {
                fillerWordCount.put(word, count);
            }
        }
        return fillerWordCount;
    }

    /**
     * 从分析块中提取与每个问题相关的眼神交流数据。
     */
    private Map<String, Double> extractEyeContactData(List<InterviewAnalysisChunk> chunks, List<InterviewDialogue> dialogues) {
        Map<String, Double> eyeContactData = new LinkedHashMap<>(); // 保持问题顺序
        if (CollectionUtils.isEmpty(chunks) || CollectionUtils.isEmpty(dialogues)) return eyeContactData;

        // 创建一个从 dialogueId 到 问题序号 的映射，方便查找
        Map<String, Integer> dialogueIdToSequenceMap = dialogues.stream()
                .collect(Collectors.toMap(d -> String.valueOf(d.getId()), InterviewDialogue::getSequence, (a, b) -> a));

        // 按 dialogueId 对 chunks 进行分组
        Map<String, List<InterviewAnalysisChunk>> chunksByDialogue = chunks.stream()
                .filter(c -> c.getDialogueId() != null)
                .collect(Collectors.groupingBy(InterviewAnalysisChunk::getDialogueId));

        // 计算每个对话轮次的平均注意力分数
        chunksByDialogue.forEach((dialogueId, dialogueChunks) -> {
            double totalAttention = 0;
            int validChunks = 0;
            for (InterviewAnalysisChunk chunk : dialogueChunks) {
                if (chunk.getPythonAnalysisReport() == null) continue;
                try {
                    JsonNode rootNode = objectMapper.readTree(chunk.getPythonAnalysisReport());
                    if (rootNode.has("attention_mean")) {
                        totalAttention += rootNode.get("attention_mean").asDouble();
                        validChunks++;
                    }
                } catch (IOException e) {
                    log.error("解析python_analysis_report失败, chunkId: {}", chunk.getId(), e);
                }
            }

            if (validChunks > 0) {
                double avgAttention = totalAttention / validChunks;
                Integer sequence = dialogueIdToSequenceMap.get(dialogueId);
                if (sequence != null) {
                    eyeContactData.put("问题" + sequence, Math.round(avgAttention * 100.0) / 100.0); // 保留两位小数
                }
            }
        });

        return eyeContactData;
    }

    private static final String FIRST_GREETING_TEXT = "你好，我是本次面试的AI面试官。为了更好地了解你，请先花一到两分钟做一个简单的自我介绍吧。";

    /**
     * 为指定的面试会话生成开场白，并通过WebSocket将语音推送给用户。
     * 此方法是异步的，会立即返回。
     *
     * @param sessionId 面试会话ID
     * @param userId    要接收语音的用户ID
     */
    @Transactional
    public void generateAndSendGreetingAudio(String sessionId, Integer userId) {

        // 1. 将开场白存入数据库
        boolean judge = saveFirstGreetingToDatabase(sessionId);
        if (judge) {
            //证明已经有了一次对话了，不需要再返回自我介绍了
            // 直接发送结束信号
            log.info("会话 {} 的开场白TTS合成完毕，发送结束信号。", sessionId);
            // 发送一个isFinal为true的空包，告诉前端这段语音结束了
            AudioResponseDto finalDto = new AudioResponseDto(null, true);
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/interview/audio.reply",
                    finalDto
            );
            return;
        }
        // 2. 异步调用TTS服务，并通过WebSocket推送结果
        try {
            log.info("开始为会话 {} 生成开场白，并将通过WebSocket推送给用户 {}", sessionId, userId);

            // 定义TTS成功接收到音频块时的回调
            Consumer<String> onAudioReceived = audioBase64 -> {
                // 将每个音频块封装成DTO，isFinal为false
                AudioResponseDto audioDto = new AudioResponseDto(audioBase64, false);
                messagingTemplate.convertAndSendToUser(
                        userId.toString(),
                        "/queue/interview/audio.reply",
                        audioDto
                );
            };
            // 定义TTS合成完成时的回调
            Runnable onComplete = () -> {
                log.info("会话 {} 的开场白TTS合成完毕，发送结束信号。", sessionId);
                // 发送一个isFinal为true的空包，告诉前端这段语音结束了
                AudioResponseDto finalDto = new AudioResponseDto(null, true);
                messagingTemplate.convertAndSendToUser(
                        userId.toString(),
                        "/queue/interview/audio.reply",
                        finalDto
                );
            };

            // 定义TTS发生错误时的回调
            Consumer<String> onError = errorMsg -> {
                log.error("会话 {} 的开场白TTS合成失败: {}", sessionId, errorMsg);
                // 同样可以发送一个结束信号，让前端停止等待
                onComplete.run();
            };

            // 调用异步的TTS服务，后续操作由回调函数处理
            ttsService.synthesize(FIRST_GREETING_TEXT, onAudioReceived, onComplete, onError);

        } catch (Exception e) {
            log.error("调用TTS服务生成开场白语音时发生异常, sessionId: {}", sessionId, e);

        }
    }
    @Resource
    private  InterviewSessionsMapper interviewSessionMapper;
    @Resource
    private  InterviewChannelsMapper interviewChannelMapper;
    @Override
    public List<InterviewHistoryCardDTO> getHistoryForUser(Integer userId) {
        // 1. 根据用户ID查询所有相关的面试会话记录，按开始时间降序排列
        LambdaQueryWrapper<InterviewSessions> sessionQuery = new LambdaQueryWrapper<>();
        sessionQuery.eq(InterviewSessions::getUserId, userId)
                .orderByDesc(InterviewSessions::getStartedAt);
        List<InterviewSessions> sessions = interviewSessionsMapper.selectList(sessionQuery);

        if (sessions.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 提取所有不重复的 channel_id，以避免N+1查询问题
        List<Long> channelIds = sessions.stream()
                .map(InterviewSessions::getChannelId)
                .distinct()
                .collect(Collectors.toList());

        // 3. 一次性查询出所有相关的频道信息
        Map<Long, InterviewChannels> channelMap = interviewChannelMapper.selectBatchIds(channelIds)
                .stream()
                .collect(Collectors.toMap(InterviewChannels::getId, channel -> channel));

        // 4. 组装返回给前端的DTO列表
        return sessions.stream().map(session -> {
            InterviewHistoryCardDTO dto = new InterviewHistoryCardDTO();
            dto.setId(session.getId());

            // 【重要修复】增加对 startedAt 字段的空值判断，防止NullPointerException
            // 如果 startedAt 为空，则使用 createdAt 作为备用日期
            LocalDateTime interviewTime = session.getStartedAt() != null ? session.getStartedAt() : session.getCreatedAt();
            if (interviewTime != null) {
                dto.setInterviewDate(interviewTime.toLocalDate());
            }

            dto.setStatus(mapStatusToFrontend(session.getStatus())); // 状态映射
            dto.setProgress(calculateProgress(session.getStatus())); // 进度计算
            dto.setOverallScore(session.getOverallScore());

            // 从Map中获取对应的频道信息
            InterviewChannels channel = channelMap.get(session.getChannelId());
            if (channel != null) {
                dto.setPosition(channel.getTargetPosition());
                dto.setInterviewType(channel.getTitle());
                dto.setImageUrl(channel.getImageUrl());
            } else {
                // 如果频道信息不存在（例如被删除或数据异常），提供默认值
                dto.setPosition("快速面试");
                dto.setInterviewType("综合面试");
            }
            return dto;
        }).collect(Collectors.toList());
    }


    /**
     * 辅助方法：将后端的中文状态映射为前端需要的英文状态
     * @param dbStatus 数据库中的状态
     * @return 前端所需的状态
     */
    private String mapStatusToFrontend(String dbStatus) {
        switch (dbStatus) {
            case "已完成":
                return "completed";
            case "进行中":
            case "准备中":
                return "pending";
            case "已取消":
                return "cancelled";
            default:
                return "unknown";
        }
    }

    /**
     * 辅助方法：根据后端状态计算面试进度
     * @param dbStatus 数据库中的状态
     * @return 进度百分比
     */
    private int calculateProgress(String dbStatus) {
        switch (dbStatus) {
            case "已完成":
                return 100;
            case "进行中":
                return 75;
            case "准备中":
                return 30;
            default:
                return 0;
        }
    }
    /**
     * 将固定的开场白作为第一轮对话存入数据库。
     *
     * @param sessionId 面试会话ID
     */
    private boolean saveFirstGreetingToDatabase(String sessionId) {
        Integer maxSequence = dialogueMapper.getMaxSequence(sessionId);
        if (maxSequence != null && maxSequence > 0) {
            log.warn("会话 {} 已存在对话记录，不再插入开场白。", sessionId);
            return true;
        }
        InterviewDialogue firstDialogue = new InterviewDialogue();
        firstDialogue.setSessionId(sessionId);
        firstDialogue.setSequence(1);
        firstDialogue.setAiMessage(FIRST_GREETING_TEXT);
        firstDialogue.setUserMessage(null);
        firstDialogue.setTimestamp(LocalDateTime.now());
        dialogueMapper.insert(firstDialogue);
        log.info("已为面试 {} 存入开场白。", sessionId);
        return false;
    }

    //完善interview_analysis_chunk的start_time_seconds、end_time_seconds、dialogue_id、
    public void fitTable(String sessionId) {
        log.info("开始对会话 sessionId: {} 进行面试后数据处理。", sessionId);

        // 1. 获取所有需要的数据
        InterviewSessions session = sessionsMapper.selectById(sessionId);
        if (session == null || session.getStartedAt() == null) {
            log.error("无法找到会话或会话开始时间为空，sessionId: {}", sessionId);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "sessionId为空");
        }
        final LocalDateTime sessionStartTime = session.getStartedAt();

        // 获取所有对话记录，并按时间戳升序排序
        List<InterviewDialogue> dialogues = dialogueMapper.selectList(
                new QueryWrapper<InterviewDialogue>()
                        .eq("session_id", sessionId)
                        .orderByAsc("timestamp")
        );

        // 获取所有分析数据块，并按创建时间升序排序
        List<InterviewAnalysisChunk> chunks = chunkMapper.selectList(
                new QueryWrapper<InterviewAnalysisChunk>()
                        .eq("session_id", sessionId)
                        .orderByAsc("created_at")
        );

        if (CollectionUtils.isEmpty(chunks)) {
            log.warn("会话 {} 没有任何分析数据块需要处理。", sessionId);
            return;
        }

        // 2. 核心处理逻辑
        List<InterviewAnalysisChunk> chunksToUpdate = new ArrayList<>();
        int dialogueIndex = 0; // 对话记录的指针

        for (InterviewAnalysisChunk chunk : chunks) {
            // --- 任务1: 填充 start_time_seconds 和 end_time_seconds ---
            LocalDateTime chunkCreatedAt = chunk.getCreatedAt();
            // 计算时间差（秒）
            long secondsFromStart = Duration.between(sessionStartTime, chunkCreatedAt).getSeconds();

            // 将秒数向下取整到最近的30秒边界
            // 例如，第 85 秒，(85 / 30) * 30 = 2 * 30 = 60
            long startTimeSeconds = (secondsFromStart / 30) * 30;
            long endTimeSeconds = startTimeSeconds + 30;

            chunk.setStartTimeSeconds((int) startTimeSeconds);
            chunk.setEndTimeSeconds((int) endTimeSeconds);

            // --- 任务2: 填充 dialogue_id ---
            if (!CollectionUtils.isEmpty(dialogues)) {
                // 移动 dialogue 指针，直到找到第一个时间戳晚于当前 chunk 的对话
                // 这样，当前 chunk 就属于指针所指向的前一个对话
                while (dialogueIndex + 1 < dialogues.size() &&
                        dialogues.get(dialogueIndex + 1).getTimestamp().isBefore(chunkCreatedAt)) {
                    dialogueIndex++;
                }
                // 将当前 chunk 关联到正确的对话轮次
                chunk.setDialogueId(String.valueOf(dialogues.get(dialogueIndex).getId()));
            }

            chunksToUpdate.add(chunk);
        }

        // 3. 批量更新数据库
        if (!chunksToUpdate.isEmpty()) {
            log.info("准备为面试会话 {} 批量更新 {} 条分析数据块...", sessionId, chunksToUpdate.size());
            chunkService.updateBatchById(chunksToUpdate);
            log.info("面试会话 {} 的分析数据块已成功更新。", sessionId);
        }


        log.info("会话 sessionId: {} 的面试后数据处理已全部完成。", sessionId);
    }

    /**
     * 辅助方法：将讯飞API返回的表情label转换为人类可读的中文描述。
     *
     * @param label 讯飞API返回的数字标签。
     * @return 对应的中文表情描述。
     */
    private String getExpressionFromLabel(int label) {
        return switch (label) {
            case 0 -> "其他(非人脸)";
            case 1 -> "其他表情";
            case 2 -> "喜悦";
            case 3 -> "愤怒";
            case 4 -> "悲伤";
            case 5 -> "惊恐";
            case 6 -> "厌恶";
            case 7 -> "中性";
            default -> "未知";
        };
    }

    /**
     * [重构后] 组装所有数据并调用buildFinalPrompt来生成最终的提示词字符串。
     *
     * @param sessionId    面试会话ID
     * @param tensionCurve 预处理好的紧张度数据
     * @param fillerWords  预处理好的口头禅数据
     * @param eyeContact   预处理好的眼神交流数据
     * @return 渲染完成的、可直接使用的提示词字符串；如果数据不足则返回null。
     */
    private String generatePrompt(String sessionId, List<Map<String, Object>> tensionCurve, Map<String, Integer> fillerWords, Map<String, Double> eyeContact) {

        // 1. 数据聚合 (这部分逻辑在调用此方法之前，在 end() 中完成，这里直接使用)
        InterviewSessions session = sessionsMapper.selectById(sessionId);
        List<InterviewDialogue> dialogues = dialogueMapper.selectList(
                new QueryWrapper<InterviewDialogue>().eq("session_id", sessionId).orderByAsc("sequence")
        );
        List<InterviewAnalysisChunk> chunks = chunkMapper.selectList(
                new QueryWrapper<InterviewAnalysisChunk>().eq("session_id", sessionId).orderByAsc("created_at")
        );

        if (CollectionUtils.isEmpty(dialogues)) {
            log.warn("会话 {} 没有对话记录，无法生成报告。", sessionId);
            return null;
        }

        // 2. 数据格式化 (不变)
        String formattedDialogues = formatDialogues(dialogues);
        String formattedAnalysisChunks = formatChunks(chunks);

        // ========================【核心修改部分】========================
        // 步骤 3: 将预处理的Java对象序列化为JSON字符串

        String tensionCurveJson = "无相关数据";
        String fillerWordsJson = "无相关数据";
        String eyeContactJson = "无相关数据";
        try {
            // 使用注入的ObjectMapper进行转换
            if (tensionCurve != null && !tensionCurve.isEmpty()) {
                tensionCurveJson = objectMapper.writeValueAsString(tensionCurve);
            }
            if (fillerWords != null && !fillerWords.isEmpty()) {
                fillerWordsJson = objectMapper.writeValueAsString(fillerWords);
            }
            if (eyeContact != null && !eyeContact.isEmpty()) {
                eyeContactJson = objectMapper.writeValueAsString(eyeContact);
            }
        } catch (IOException e) {
            log.error("序列化预处理指标时发生错误, sessionId: {}", sessionId, e);
            // 即使出错，也继续使用默认值，不中断流程
        }

        // 步骤 4: 调用更新后的 buildFinalPrompt 方法，传入所有数据
        String finalPrompt = buildFinalPrompt(
                session,
                formattedDialogues,
                formattedAnalysisChunks,
                tensionCurveJson,
                fillerWordsJson,
                eyeContactJson
        );
        // =============================================================

        log.info("为会话 {} 构建的最终提示词长度: {}", sessionId, finalPrompt.length());
        return finalPrompt;
    }

    private String formatDialogues(List<InterviewDialogue> dialogues) {
        return dialogues.stream()
                .map(d -> String.format(
                        "### 对话轮次 %d\n- **面试官提问**: %s\n- **候选人回答**: %s\n- **初步分析**: %s\n",
                        d.getSequence(),
                        d.getAiMessage(),
                        d.getUserMessage(),
                        d.getTurnAnalysis() != null ? d.getTurnAnalysis() : "无"
                ))
                .collect(Collectors.joining("\n"));
    }

    private String formatChunks(List<InterviewAnalysisChunk> chunks) {
        if (CollectionUtils.isEmpty(chunks)) {
            return "无行为分析数据。";
        }
        // 这里只做一个简单的总结，避免Prompt过长。可以根据需要提取更详细的信息。
        long positiveExpressions = chunks.stream().filter(c -> c.getIflytekExpressionReport() != null && c.getIflytekExpressionReport().contains("positive")).count();
        long neutralExpressions = chunks.stream().filter(c -> c.getIflytekExpressionReport() != null && c.getIflytekExpressionReport().contains("neutral")).count();
        long negativeExpressions = chunks.stream().filter(c -> c.getIflytekExpressionReport() != null && c.getIflytekExpressionReport().contains("negative")).count();

        return String.format(
                "在整个面试过程中，候选人的表情分析大致如下：\n- 积极表情占比: %.2f%%\n- 中性表情占比: %.2f%%\n- 消极表情占比: %.2f%%\n（具体细节已融入逐轮分析中）",
                (double) positiveExpressions / chunks.size() * 100,
                (double) neutralExpressions / chunks.size() * 100,
                (double) negativeExpressions / chunks.size() * 100
        );
    }

    private String buildFinalPrompt(
            InterviewSessions session,
            String formattedDialogues,
            String formattedAnalysisChunks,
            // 这三个参数是您在 end() 方法中通过Java代码计算好的，并已转为JSON字符串
            String tensionCurveJson,
            String fillerWordsJson,
            String eyeContactJson
    ) {
        // 1. 为注入做准备：处理可能为空的指标，确保注入的是合法的JSON空值
        String tensionForPrompt = (tensionCurveJson != null && !tensionCurveJson.equals("无数据")) ? tensionCurveJson : "[]";
        String fillerWordsForPrompt = (fillerWordsJson != null && !fillerWordsJson.equals("无数据")) ? fillerWordsJson : "{}";
        String eyeContactForPrompt = (eyeContactJson != null && !eyeContactJson.equals("无数据")) ? eyeContactJson : "{}";


        // 2. 这是整个方案的“大脑”，一个高质量的Prompt至关重要
        return String.format("""
                        # 角色
                        你是一名资深的HR专家和技术面试官，你的任务是基于提供的全面数据，生成一份专业、深度、结构化的评估报告。
                        
                        # 任务
                        你的核心任务是**补完**「请补完以下JSON对象」部分提供的JSON模板。你需要：
                        1.  仔细阅读「输出格式说明」，理解每个需要你生成的字段的深刻含义。
                        2.  基于「面试背景」、「完整对话记录」和「行为数据摘要」进行综合分析。
                        3.  将你的分析结果和评分，填入JSON模板的相应字段中。
                        
                        # 面试背景
                        - **面试岗位**: %s
                        
                        # 完整对话记录
                        ---
                        %s
                        ---
                        
                        # 行为数据摘要
                        ---
                        %s
                        ---
                        
                        # 核心指令
                        1.  **以JSON为中心**: 你的唯一目标就是补完给定的JSON对象。
                        2.  **解读并分析**: 你的分析（如`behavioral_analysis`）必须与我提供的所有数据（包括预处理指标）保持一致。
                        3.  **精确量化**: `overall_score` 必须是所有单项得分的加权平均值（专业知识和技能匹配度权重各占30%%，其他四项各占10%%）。
                        4.  **纯净输出**: 你的最终输出**必须且只能是**一个完整的、纯净的JSON对象，不包含任何解释、注释或Markdown代码块标记。
                        
                        # 在输出的格式中，各名词代表的含义是：
                        - **overall_summary**: (string) 对候选人此场面试表现的**亮点与核心不足**进行高度概括，作为给HR的快速参考。严格控制在200字以内。
                            - `score_professional_knowledge(number, 0-100)`: 评估候选人对岗位所需专业知识的掌握深度和广度。
                            - `score_skill_match(number, 0-100)`: 评估候选人简历和回答中体现的技能与岗位要求的契合程度。
                            - `score_verbal_expression(number, 0-100)`: 评估候选人语言组织的逻辑性、表达的流畅度与清晰度。
                            - `score_logical_thinking(number, 0-100)`: 评估候选人分析问题、拆解问题和推理结论的能力。
                            - `score_innovation(number, 0-100)`: 评估候选人是否能提出新颖的观点或独特的解决方案。
                            - `score_stress_resistance(number, 0-100)`: 评估候选人在面对追问或难题时的情绪稳定性和应变能力。
                        - **behavioral_analysis**: (string) 一段详细的非语言行为分析。必须结合「行为数据摘要」和「预处理的关键行为指标」来描述候选人的情绪状态、专注度、自信心等。
                        - **behavioral_suggestions**: (string) 基于`behavioral_analysis`的结果，提出1-2条具体的、可操作的行为改进建议。
                        - **overall_suggestions**: (string) 给候选人的一份全面的、富有同理心和建设性的综合发展建议，帮助其长期成长。
                        - **overall_score**: (number) 根据核心指令中的权重计算出的最终加权平均总分，可保留一位小数。
                        - **overall_analysis**: (string) 一段比`overall_summary`更详尽的最终总结性分析，深入阐述候选人的综合能力画像、长处与短板，并给出最终的倾向性建议。
 
                        """,
                session.getChannelId() != null ? "一个指定的岗位" : "一个通用技术岗位",
                formattedDialogues,
                formattedAnalysisChunks
                // 按照顺序注入准备好的JSON字符串
        );
    }

    private void saveReportData(
            InterviewSessions session,
            AnalysisReportDTO dto,
            List<Map<String, Object>> tensionCurve,
            Map<String, Integer> fillerWords,
            Map<String, Double> eyeContact
    ) {
        // 保存到 analysis_reports 表
        AnalysisReports report = new AnalysisReports();
        // report.setSessionId(Long.valueOf(session.getId())); // 如果 session.getId() 是 String，而 report.getSessionId() 是 Long，需要转换
        report.setSessionId(session.getId()); // 假设类型匹配
        report.setGeneratedAt(LocalDateTime.now());

        // 这些字段由AI生成，我们信任AI的分析和总结能力，所以直接从DTO获取
        report.setOverallSummary(dto.overallSummary());
        report.setScoreProfessionalKnowledge(dto.scoreProfessionalKnowledge());
        report.setScoreSkillMatch(dto.scoreSkillMatch());
        report.setScoreVerbalExpression(dto.scoreVerbalExpression());
        report.setScoreLogicalThinking(dto.scoreLogicalThinking());
        report.setScoreInnovation(dto.scoreInnovation());
        report.setScoreStressResistance(dto.scoreStressResistance());
        report.setBehavioralAnalysis(dto.behavioralAnalysis());
        report.setBehavioralSuggestions(dto.behavioralSuggestions());
        report.setOverallSuggestions(dto.overallSuggestions());

        // 序列化JSON字段
        try {
            // 【核心修改】对于这三个指标，我们使用Java代码精确计算的结果，而不是AI返回的结果
            report.setTensionCurveData(objectMapper.writeValueAsString(tensionCurve));
            report.setFillerWordUsage(objectMapper.writeValueAsString(fillerWords));
            report.setEyeContactPercentage(objectMapper.writeValueAsString(eyeContact));

//            // 对于AI独有分析的部分（如逐题分析），我们仍然使用DTO中的数据
//            report.setQuestionAnalysisData(objectMapper.writeValueAsString(dto.questionAnalysisData()));

        } catch (Exception e) {
            log.error("序列化报告JSON字段失败, sessionId: {}", session.getId(), e);
        }

        reportsMapper.insert(report);

        // 更新 interview_sessions 表
        session.setOverallScore(dto.overallScore());
        session.setOverallAnalysis(dto.overallAnalysis());
        session.setStatus("已完成"); // 更新面试状态
        session.setEndedAt(LocalDateTime.now());
        sessionsMapper.updateById(session);

        log.info("已成功将最终分析报告存入数据库, sessionId: {}", session.getId());
    }
}
