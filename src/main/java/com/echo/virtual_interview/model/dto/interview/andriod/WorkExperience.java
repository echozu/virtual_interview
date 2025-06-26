package com.echo.virtual_interview.model.dto.interview.andriod;

import lombok.Data;

import java.io.Serializable;

@Data
public class WorkExperience implements Serializable {
    private String id;
    private String companyName;
    private String industry;
    private String startDate;
    private String endDate;
    private Boolean isCurrentJob;
    private String position;
    private String workContent;
    private String achievements;
    private String department;
    private Boolean isInternship;
}
