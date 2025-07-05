package com.echo.virtual_interview.service.impl;

import com.echo.virtual_interview.common.ErrorCode;
import com.echo.virtual_interview.context.UserIdContext;
import com.echo.virtual_interview.controller.ai.InterviewExpert;
import com.echo.virtual_interview.exception.BusinessException;
import com.echo.virtual_interview.mapper.InterviewAnalysisChunkMapper;
import com.echo.virtual_interview.model.dto.interview.process.IflytekExpressionResponse;
import com.echo.virtual_interview.model.dto.interview.process.RealtimeFeedbackDto;
import com.echo.virtual_interview.model.dto.interview.process.VideoAnalysisPayload;
import com.echo.virtual_interview.model.entity.InterviewAnalysisChunk;
import com.echo.virtual_interview.model.entity.InterviewSessions;
import com.echo.virtual_interview.mapper.InterviewSessionsMapper;
import com.echo.virtual_interview.service.IInterviewSessionsService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.echo.virtual_interview.utils.face.IflytekExpressionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * <p>
 * 记录每一次具体的面试实例 服务实现类
 * </p>
 *
 * @author echo
 * @since 2025-06-20
 */
@Service
@Transactional
@Slf4j
public class InterviewSessionsServiceImpl extends ServiceImpl<InterviewSessionsMapper, InterviewSessions>
        implements IInterviewSessionsService {

    @Resource
    private ResumeServiceImpl resumeService;
    @Resource
    private InterviewSessionsMapper interviewSessionsMapper;
    @Resource
    private InterviewExpert interviewExpert;
    @Resource
    private ObjectMapper objectMapper;
    @Resource
    private InterviewAnalysisChunkMapper analysisChunkMapper;

    @Resource
    private IflytekExpressionService iflytekExpressionService; // 注入新创建的讯飞服务
    @Override
    public String start(Long channelId) {
        Integer userId = UserIdContext.getUserIdContext();
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        // 获取简历ID（处理可能的空值）
        Long resumeId = Optional.ofNullable(resumeService.getResumeIdByUserId(userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.OPERATION_ERROR,"请先上传简历"));

        // 创建会话ID
        String sessionId = String.format("%d-%s-%d",
                userId, channelId, System.currentTimeMillis());

        // 保存会话记录
        InterviewSessions session = new InterviewSessions()
                .setId(sessionId)
                .setChannelId(channelId)
                .setUserId(userId.longValue())
                .setResumeId(resumeId)
                .setStatus("进行中");

        interviewSessionsMapper.insert(session);
        return sessionId;
    }

    /**
     * 实现了完整的处理流程：
     * 1. 调用讯飞API分析图片。
     * 2. 整合Python视频分析结果和图片分析结果。
     * 3. 调用大模型生成简短的实时反馈。
     *
     * @param payload 从Python服务接收到的完整数据。
     * @return AI生成的实时反馈字符串，或在失败时返回null。
     */
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
            return null;
        }
        var latestReport = payload.getAnalysisByHalfMinute().get(payload.getAnalysisByHalfMinute().size() - 1);

        StringBuilder dataForAI = new StringBuilder();
        dataForAI.append(String.format("- **核心紧张度**: %s\n", latestReport.getNervousnessAnalysis().getLevel()));
        dataForAI.append(String.format("- **平均注意力分数**: %.2f\n", latestReport.getAttentionMean()));
        dataForAI.append(String.format("- **注意力稳定性**: %.3f\n", latestReport.getAttentionStability()));
        dataForAI.append(String.format("- **总眨眼次数**: %d\n", latestReport.getTotalBlinks()));
        dataForAI.append(String.format("- **头部晃动幅度**: %.2f\n", latestReport.getHeadPose().getYawFluctuation()));
        firstFrameAnalysisOpt.ifPresent(res -> res.data().fileList().stream().findFirst().ifPresent(file -> dataForAI.append(String.format("- **开场表情**: %s\n", getExpressionFromLabel(file.label())))));
        lastFrameAnalysisOpt.ifPresent(res -> res.data().fileList().stream().findFirst().ifPresent(file -> dataForAI.append(String.format("- **当前表情**: %s\n", getExpressionFromLabel(file.label())))));

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
            expressionReport.put("first_frame_analysis", firstFrameAnalysisOpt.orElse(null));
            expressionReport.put("last_frame_analysis", lastFrameAnalysisOpt.orElse(null));
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
}
