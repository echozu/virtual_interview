package com.echo.virtual_interview.model.dto.resum;

import lombok.Data;
import java.util.List;

@Data
public class BasicInfoDto {
    private String title;
    private String cover;
    private String tag;
    private String avatar;
    private List<BasicInfoItemDto> basicInfos;
}