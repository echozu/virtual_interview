package com.echo.virtual_interview.model.dto.interview.andriod;

import lombok.Data;

import java.io.Serializable;

@Data
public class EducationExperience implements Serializable {
    private String id;
    private String school;
    private String degree;
    private String major;
    private String startDate;
    private String endDate;
    private String campusExperience;
    private Boolean isJointProgram;
}
