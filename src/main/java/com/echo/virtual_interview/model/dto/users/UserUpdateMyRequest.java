package com.echo.virtual_interview.model.dto.users;

import java.io.Serializable;
import lombok.Data;

/**
 * 用户更新个人信息请求
 *
 *  
 */
@Data
public class UserUpdateMyRequest implements Serializable {

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 用户头像
     */
    private String avatarUrl;

    /**
     * 用户简介
     */
    private String profile;


    private static final long serialVersionUID = 1L;
}