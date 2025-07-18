package com.echo.virtual_interview.model.dto.history;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 用于前端面试历史记录卡片展示的DTO
 */
@Data
public class InterviewHistoryCardDTO {

    /**
     * 面试会话的唯一ID
     */
    private String id;

    /**
     * 目标岗位 (来自 interview_channels.target_position)
     */
    private String position;

    /**
     * 面试类型/标题 (来自 interview_channels.title)
     */
    private String interviewType;

    /**
     * 面试日期 (来自 interview_sessions.started_at)
     */
    private LocalDate interviewDate;

    /**
     * 面试状态 (来自 interview_sessions.status)
     * 前端可以根据此字段显示不同状态，如 'completed', 'pending' 等
     */
    private String status;

    /**
     * 面试进度 (根据status计算)
     */
    private int progress;

    /**
     * 【新增】综合得分 (来自 interview_sessions.overall_score)
     * 这是一个非常关键的反馈信息，适合在卡片上展示。
     */
    private BigDecimal overallScore;

    /**
     * 【新增】频道封面图 (来自 interview_channels.image_url)
     * 让卡片展示更美观。
     */
    private String imageUrl;
}