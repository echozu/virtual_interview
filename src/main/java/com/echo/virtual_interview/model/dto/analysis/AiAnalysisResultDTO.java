package com.echo.virtual_interview.model.dto.analysis;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 用于解析AI返回的JSON (已增加雷达图评分)
 */
@Data
public class AiAnalysisResultDTO {
    public String overall_summary;
    public String behavioral_analysis;
    public String behavioral_suggestions;
    public String overall_suggestions;
    public RadarScores radar_scores; // 新增：用于接收AI返回的雷达图分数
    public List<WeakTopicWithReason> weak_topics_with_reasons;

    /**
     * 雷达图六个维度的分数
     */
    @Data
    public static class RadarScores {
        public BigDecimal professional_knowledge;
        public BigDecimal skill_match;
        public BigDecimal verbal_expression;
        public BigDecimal logical_thinking;
        public BigDecimal innovation;
        public BigDecimal stress_resistance;
    }

    @Data
    public static class WeakTopicWithReason {
        public String topic;
        public String recommendation_reason;
    }
}