package com.echo.virtual_interview.model.dto.analysis;

import lombok.Data;

// 问题分析
@Data
public class QuestionAnalysisDTO {
    private String question;
    private String answer;
    private Integer score;
    private String suggestion;
}
