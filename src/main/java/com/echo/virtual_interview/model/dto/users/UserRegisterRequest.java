package com.echo.virtual_interview.model.dto.users;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户注册请求体
 *
 *  
 */
@Data
public class UserRegisterRequest implements Serializable {

    private static final long serialVersionUID = 3191241716373120793L;

    private String username;

    private String userPassword;

    private String checkPassword; //二次验证的密码

    private String email;
    private String captcha; // 验证码
    private String cache; // 安卓的添加的验证码字段

}
