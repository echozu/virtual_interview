package com.echo.virtual_interview.model.dto.career;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
public class CareerProfileVo {
    private Long userId;
    private String nickname;
    private String avatarUrl;
    private Integer totalInterviews;
    private Double averageOverallScore;
    private String generatedAt;
    private RadarChartData competencyRadarChart;
    private AiAnalysis aiAnalysis;

    @Data
    public static class RadarChartData {
        private List<String> labels;
        private List<Double> values;
    }
    
    @Data
    public static class AiAnalysis {
        private String strengths;
        private String areasForImprovement;
        private String behavioralHabits;
        private String careerDevelopmentSuggestions;
        private String suitablePositions;
        private String positionFitAnalysis;

    }

    public static CareerProfileVo empty(Long userId) {
        CareerProfileVo vo = new CareerProfileVo();
        vo.setUserId(userId);
        // 可以设置一些默认提示信息
        return vo;
    }
}