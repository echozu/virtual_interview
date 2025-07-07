package com.echo.virtual_interview.model.dto.interview;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

// 这个DTO类用于接收AI生成的结构化JSON报告
public record AnalysisReportDTO(
    @JsonProperty("overall_summary") String overallSummary,
    @JsonProperty("score_professional_knowledge") BigDecimal scoreProfessionalKnowledge,
    @JsonProperty("score_skill_match") BigDecimal scoreSkillMatch,
    @JsonProperty("score_verbal_expression") BigDecimal scoreVerbalExpression,
    @JsonProperty("score_logical_thinking") BigDecimal scoreLogicalThinking,
    @JsonProperty("score_innovation") BigDecimal scoreInnovation,
    @JsonProperty("score_stress_resistance") BigDecimal scoreStressResistance,
    @JsonProperty("behavioral_analysis") String behavioralAnalysis,
    @JsonProperty("behavioral_suggestions") String behavioralSuggestions,
    @JsonProperty("tension_curve_data") List<TensionPoint> tensionCurveData,
    @JsonProperty("overall_suggestions") String overallSuggestions,
    @JsonProperty("filler_word_usage") Map<String, Integer> fillerWordUsage,
    @JsonProperty("eye_contact_percentage") Map<String, Double> eyeContactPercentage,
    @JsonProperty("overall_score") BigDecimal overallScore,
    @JsonProperty("overall_analysis") String overallAnalysis
) {
    // 内部记录，用于JSON数组的结构化
    public record TensionPoint(String time, int value) {}
    public record QuestionAnalysis(String question, String answer, BigDecimal score, String suggestion) {}
}