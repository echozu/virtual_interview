// ResumeData.java
package com.echo.virtual_interview.model.dto.interview.andriod;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class ResumeData implements Serializable {
    private ResumeBasicInfo basicInfo;
    private String jobStatus;
    private PersonalAdvantage personalAdvantage;
    private List<JobExpectation> jobExpectations;
    private List<WorkExperience> workExperiences;
    private List<ProjectExperience> projectExperiences;
    private List<EducationExperience> educationExperiences;
}


