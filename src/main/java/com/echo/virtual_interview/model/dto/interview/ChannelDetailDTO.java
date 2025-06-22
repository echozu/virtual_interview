package com.echo.virtual_interview.model.dto.interview;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ChannelDetailDTO {
    // 包含所有 InterviewChannel 的字段
    private Long id;
    private Long creatorId;
    private String title;
    private String description;
    private String major;
    private String jobType;
    private String targetPosition;
    private String interviewerStyle;
    private String targetCompany;
    private String interviewMode;
    private Integer estimatedDuration;
    private String visibility;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String imageUrl;
    private Integer usageCount;

    // 【新增】用于存放关联的知识点列表
    private List<String> topics;
}