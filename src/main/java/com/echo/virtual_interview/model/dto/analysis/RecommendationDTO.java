package com.echo.virtual_interview.model.dto.analysis;

import lombok.Data;

// 学习推荐
@Data
public class RecommendationDTO {
    private Long id;
    private String title;
    private String recommendation_reason;
    private String url;
    private Integer step_number;
}
