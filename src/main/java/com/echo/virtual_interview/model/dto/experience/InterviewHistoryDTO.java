package com.echo.virtual_interview.model.dto.experience;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用于选择的历史面试记录 DTO
 * <p>
 * 在发表面经时，前端会展示此DTO列表，让用户选择为哪一场面试撰写经验。
 */
@Data
public class InterviewHistoryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * **核心字段**: 面试会话的唯一ID，前端选择后需将此ID传给创建面经的接口。
     */
    private String sessionId;

    /**
     * **主要识别信息**: 面试的标题，例如 "阿里巴巴-Java后端-校招一面"。
     * 这个字段能让用户最直观地识别是哪场面试。
     */
    private String interviewTitle;

    /**
     * **辅助识别信息**: 面试开始的时间。
     * 当标题相近时，用户可以通过时间来区分。
     */
    private LocalDateTime interviewDate;
    
    /**
     * **参考信息**: 该场面试的综合得分。
     * 可以帮助用户回忆起面试表现。
     */
    private BigDecimal overallScore;

    /**
     * **状态字段**: 该场面试是否已经发表过面经。
     * true表示已发表，前端可以将对应的选项置灰，防止重复为一场面试创建多篇面经。
     */
    private boolean hasExperiencePost;
}