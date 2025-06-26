package com.echo.virtual_interview.model.dto.interview.andriod;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class JobExpectation implements Serializable {
    private String id;
    private String position;
    private String salary;
    private List<String> cities;
    private String industry;
}
