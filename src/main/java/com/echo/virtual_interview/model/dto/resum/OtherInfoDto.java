package com.echo.virtual_interview.model.dto.resum;

import lombok.Data;
import java.util.List;

@Data
public class OtherInfoDto {
    private String id; // 前端使用的UUID
    private Integer sort;
    private String title;
    private Boolean isAppear;
    private String fromDate;
    private String toDate;
    private Boolean toNow;
    private String subject;
    private String major;
    private String content;
    // 递归结构
    private List<OtherInfoDto> children;
}