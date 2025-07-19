package com.echo.virtual_interview.model.dto.experience;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SharedReportElementsDTO {
    @JsonProperty("overall_summary")
    private boolean overallSummary;
    @JsonProperty("radar_data")
    private boolean radarData;
    @JsonProperty("behavioral_analysis")
    private boolean behavioralAnalysis;
    @JsonProperty("overall_suggestions")
    private boolean overallSuggestions;
    @JsonProperty("behavioral_suggestions")
    private boolean behavioralSuggestions;
    @JsonProperty("question_analysis_data")
    private boolean questionAnalysisData;
    @JsonProperty("tension_curve_data")
    private boolean tensionCurveData;
    @JsonProperty("filler_word_usage")
    private boolean fillerWordUsage;
    @JsonProperty("eye_contact_percentage")
    private boolean eyeContactPercentage;
    @JsonProperty("recommendations")
    private boolean recommendations;
}