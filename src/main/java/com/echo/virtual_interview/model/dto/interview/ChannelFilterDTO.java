package com.echo.virtual_interview.model.dto.interview;

import lombok.Data;

import java.util.List;

@Data
public class ChannelFilterDTO {
    // 分页参数
    private long pageNum = 1;
    private long pageSize = 10;

    // 筛选条件
    private String jobType;
    private String interviewerStyle;
    private String interviewMode;
    private Integer estimatedDuration;
    private String major;
    private String targetPosition;
    private String targetCompany;
    
    // topics 可以支持多选，或者模糊匹配一个
    private String topic; 

    // 排序条件: "asc" 或 "desc"
    private String usageCountSort ="asc";
}