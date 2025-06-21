package com.echo.virtual_interview.exception;


import com.echo.virtual_interview.common.BaseResponse;
import com.echo.virtual_interview.common.ErrorCode;
import com.echo.virtual_interview.common.ResultUtils;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器、
 * 避免出现乱七八糟的信息返回给前端
 *如果是乱七八糟的异常，这里就会去捕获，然后进行处理（如1/0）
 */
@Hidden
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    //如果哪里抛出了：BusinessException 那么就会被捕获并且去封装，返回给前端
    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> businessExceptionHandler(BusinessException e) {
        log.error("BusinessException", e); //打印日志
        return ResultUtils.error(e.getCode(), e.getMessage()); //封装好看的给前端
    }
    //捕获运行时异常
    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> runtimeExceptionHandler(RuntimeException e) {
        log.error("RuntimeException", e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统错误");
    }
}
