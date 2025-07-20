package com.echo.virtual_interview.model.dto.career;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("career_assessments")
public class CareerAssessment {
    private Long id;
    private Long userId;
    private Integer totalInterviewsAnalyzed;
    private BigDecimal averageOverallScore;
    private String competencyRadarData; // JSON String
    private String strengthsSummary;
    private String improvementSummary;
    private String behavioralHabitsSummary;
    private String careerSuggestions;
    private String suitablePositions;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableField("position_fit_analysis") // 建议明确指定数据库列名
    private String positionFitAnalysis;
}