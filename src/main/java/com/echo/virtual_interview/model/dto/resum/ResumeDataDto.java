package com.echo.virtual_interview.model.dto.resum;

import lombok.Data;
import java.util.List;

@Data
public class ResumeDataDto {
    private BasicInfoDto basicInfo;
    private List<OtherInfoDto> otherInfos;
}