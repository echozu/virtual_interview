package com.echo.virtual_interview.model.dto.sms;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 短信验证码请求数据传输对象
 * 
 * @author Heritage Team
 * @description 用于接收发送验证码请求的数据
 */
@Data
@Schema(description = "验证码发送请求")
public class SmsRequest {

    @Schema(description = "邮箱地址", example = "user@example.com")
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;
}
