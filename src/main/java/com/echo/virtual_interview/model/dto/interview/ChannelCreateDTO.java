package com.echo.virtual_interview.model.dto.interview;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ChannelCreateDTO {

    @NotBlank(message = "频道标题不能为空")
    @Size(max = 100, message = "标题长度不能超过100个字符")
    private String title;

    private String description;

    private String major;

    private List<String> topics;

    @NotBlank(message = "工作类型不能为空")
    private String jobType;

    private String targetPosition;

    private String interviewerStyle;

    private String targetCompany;

    private String interviewMode;

    private Integer estimatedDuration;

    private String imageUrl;

    @NotNull(message = "必须指定是否申请公开,选择公开标签则将状态改为审核中")
    private Boolean requestPublic;
}