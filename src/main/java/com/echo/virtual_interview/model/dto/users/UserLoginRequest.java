package com.echo.virtual_interview.model.dto.users;

import java.io.Serializable;
import lombok.Data;

/**
 * 用户登录请求
 *
 *  
 */
@Data
public class UserLoginRequest implements Serializable {

    private static final long serialVersionUID = 3191241716373120793L;
    private String password;
    private String username; // 可以传来username或者邮箱进行登录
    private String email;
}
