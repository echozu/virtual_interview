package com.echo.virtual_interview.controller;

import com.echo.virtual_interview.common.BaseResponse;
import com.echo.virtual_interview.common.ErrorCode;
import com.echo.virtual_interview.common.ResultUtils;
import com.echo.virtual_interview.model.dto.sms.SmsRequest;
import com.echo.virtual_interview.service.impl.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 账户相关控制器
 */
@RestController
@Tag(name = "账户管理", description = "验证码发送等账户相关接口")
@RequestMapping("/account")
public class AccountController {

    @Autowired
    private EmailService emailService;

    @PostMapping("/sms")
    @Operation(summary = "发送邮箱验证码", description = "向指定邮箱发送6位数字验证码，有效期5分钟")
    public BaseResponse<String> sendCaptcha(@Valid @RequestBody SmsRequest smsRequest) {
        String email = smsRequest.getEmail();

        // 校验邮箱格式
        if (email == null || !email.matches("^[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+$")) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR, "邮箱格式不正确");
        }

        try {
            emailService.generateAndSendCaptcha(email);
            return ResultUtils.success("验证码已发送，请查收邮件");
        } catch (Exception e) {
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "验证码发送失败：" + e.getMessage());
        }
    }
}