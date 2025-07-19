package com.echo.virtual_interview.model.dto.experience;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 面经详情页的响应体 (Response DTO)
 * 由Service层组装各类信息而成
 */
@Data
public class ExperiencePostDetailResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    // --- 面经本身的基础信息 ---
    private Long id;
    private String title;
    private String content; // 完整内容
    private String visibility;
    private Boolean isAnonymous;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String experienceUrl;
    // --- 作者信息 ---
    private AuthorInfoVO authorInfo;

    // --- 统计数据 ---
    private Integer viewsCount;
    private Integer likesCount;
    private Integer commentsCount;
    private Integer collectionsCount;

    // --- 标签信息 ---
    private List<String> tags;

    // --- 关联的AI面试报告摘要 ---
    private AiReportSummaryVO aiReportSummary;
    
    // --- 关联的面试频道信息 (用于"一键同款面试") ---
    private InterviewChannelInfoVO channelInfo;

    // --- 当前登录用户的互动状态 ---
    private Boolean isLiked; // 当前用户是否已点赞
    private Boolean isCollected; // 当前用户是否已收藏

    /**
     * 作者信息内部VO
     */
    @Data
    public static class AuthorInfoVO implements Serializable {
        private Integer authorId;
        private String nickname;
        private String avatarUrl;
    }

    /**
     * AI报告摘要内部VO
     */
    @Data
    public static class AiReportSummaryVO implements Serializable {
        private String sessionId;
        private java.math.BigDecimal overallScore; // 总体得分
        private String highlights; // 亮点总结
        private String suggestions; // 改进建议
        private Map<String, Object> radarChartData; // 雷达图数据, e.g., {"专业知识": 90, "逻辑思维": 85...}
    }
    
    /**
     * 面试频道信息内部VO
     */
    @Data
    public static class InterviewChannelInfoVO implements Serializable {
        private Long channelId;
        private String title;
    }
}