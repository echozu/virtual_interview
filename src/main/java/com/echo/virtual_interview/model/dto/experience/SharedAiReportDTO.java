package com.echo.virtual_interview.model.dto.experience;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL) // 如果字段为null，则不序列化
@JsonIgnoreProperties(ignoreUnknown = true) // 至关重要！用于在解析完整报告时，忽略DTO中不存在的字段（如"dialogues"）
public class SharedAiReportDTO {

    // --- 基础信息（始终展示） ---
    private String position;
    private String interviewType;
    private String interviewDate;

    // --- 以下为可选模块 ---
    @JsonProperty("overall_summary")
    private String overallSummary;

    @JsonProperty("radar_data")
    private RadarDataDTO radarData;

    @JsonProperty("behavioral_analysis")
    private String behavioralAnalysis;

    @JsonProperty("overall_suggestions")
    private String overallSuggestions;

    @JsonProperty("behavioral_suggestions")
    private String behavioralSuggestions;

    @JsonProperty("question_analysis_data")
    private List<QuestionAnalysisDTO> questionAnalysisData;

    @JsonProperty("tension_curve_data")
    private CurveDataDTO tensionCurveData;

    @JsonProperty("filler_word_usage")
    private CurveDataDTO fillerWordUsage;

    @JsonProperty("eye_contact_percentage")
    private CurveDataDTO eyeContactPercentage;

    private List<RecommendationDTO> recommendations;

    // --- 内部嵌套DTO ---
    @Data
    public static class RadarDataDTO {
        private List<Map<String, Object>> series;
        private List<Map<String, Object>> indicator;
    }

    @Data
    public static class QuestionAnalysisDTO {
        private String question;
        private String answer;
        private Integer score;
        private String suggestion;
    }

    @Data
    public static class CurveDataDTO {
        private List<String> xaxisData;
        private List<Double> seriesData;
    }

    @Data
    public static class RecommendationDTO {
        private Integer id;
        @JsonProperty("step_number")
        private Integer stepNumber;
        private String title;
        private String url;
        @JsonProperty("recommendation_reason")
        private String recommendationReason;
    }
}