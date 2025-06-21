package com.echo.virtual_interview.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResultVO {
    private LoginUserVO userInfo;
    private String token;
}