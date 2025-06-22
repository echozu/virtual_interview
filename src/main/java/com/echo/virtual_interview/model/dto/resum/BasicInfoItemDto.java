package com.echo.virtual_interview.model.dto.resum;

import lombok.Data;

@Data
public class BasicInfoItemDto {
    private String id; // 前端使用的UUID
    private String basicKey;
    private String basicVal;
}