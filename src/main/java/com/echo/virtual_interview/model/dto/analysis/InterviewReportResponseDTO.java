package com.echo.virtual_interview.model.dto.analysis;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

// 总的报告响应体
@Data
public class InterviewReportResponseDTO {
    // 基本信息
    private String position;
    private String interviewType;
    private String interviewDate;
    private String generated_at;

    // AI生成的分析文本
    private String overall_summary;
    private String behavioral_analysis;
    private String behavioral_suggestions;
    private String overall_suggestions;

    // 逐题分析
    private List<QuestionAnalysisDTO> question_analysis_data;

    // 对话记录
    private List<DialogueDTO> dialogues;

    // 学习推荐
    private List<RecommendationDTO> recommendations;

    // 雷达图数据
    private RadarDataDTO radar_data;

    // 图表数据
    private ChartDataDTO tension_curve_data;
    private ChartDataDTO filler_word_usage;
    private ChartDataDTO eye_contact_percentage;
}

