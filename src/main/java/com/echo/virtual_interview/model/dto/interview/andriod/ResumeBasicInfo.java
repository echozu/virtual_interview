package com.echo.virtual_interview.model.dto.interview.andriod;

import lombok.Data;

import java.io.Serializable;

@Data
public class ResumeBasicInfo implements Serializable {
    private String title;
    private String cover;
    private String tag;
    private String avatar;
    private String name;
    private String graduationYear;
    private String age;
    private String education;
    private String phone;
    private String wechat;
    private String gender;
    private String identity;
    private String birthDate;
    private String birthPlace;
    private String email;
}
