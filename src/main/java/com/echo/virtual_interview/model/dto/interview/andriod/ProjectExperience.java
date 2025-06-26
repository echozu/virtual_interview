package com.echo.virtual_interview.model.dto.interview.andriod;

import lombok.Data;

import java.io.Serializable;

@Data
public class ProjectExperience implements Serializable {
    private String id;
    private String projectName;
    private String role;
    private String startDate;
    private String endDate;
    private String description;
    private String achievements;
    private String projectLinks;
}
