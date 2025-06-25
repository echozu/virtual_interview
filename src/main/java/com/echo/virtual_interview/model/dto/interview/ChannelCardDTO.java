package com.echo.virtual_interview.model.dto.interview;

import lombok.Data;

@Data
public class ChannelCardDTO {
    private Long id;
    private String title;
    private String imageUrl;
    private String targetCompany;
    private String targetPosition;
    private Integer estimatedDuration;
    private Integer usageCount;
    //新增的字段
    private String jobType;
    private String interviewerStyle;
    private String interviewMode;
}