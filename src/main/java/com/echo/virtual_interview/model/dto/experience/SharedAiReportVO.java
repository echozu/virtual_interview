package com.echo.virtual_interview.model.dto.experience;

import com.echo.virtual_interview.model.dto.analysis.ChartDataDTO;
import com.echo.virtual_interview.model.dto.analysis.QuestionAnalysisDTO;
import com.echo.virtual_interview.model.dto.analysis.RadarDataDTO;
import com.echo.virtual_interview.model.dto.analysis.RecommendationDTO;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 经过用户分享偏好筛选后，最终展示在面经详情页的AI报告摘要。
 * 这是一个类型安全的视图对象 (View Object)。
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL) // 关键：如果某个字段为null，则不在最终的JSON中显示
public class SharedAiReportVO implements Serializable {

    private static final long serialVersionUID = 1L;

    // AI生成的分析文本
    private String overallSummary;
    private String behavioralAnalysis;
    private String overallSuggestions;

    // 逐题分析
    private List<QuestionAnalysisDTO> questionAnalysisData;

    // 学习推荐
    private List<RecommendationDTO> recommendations;

    // 雷达图数据
    private RadarDataDTO radarData;

    // 图表数据
    private ChartDataDTO tensionCurveData;
    private ChartDataDTO fillerWordUsage;
}