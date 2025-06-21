package com.echo.virtual_interview.exception;


import com.echo.virtual_interview.common.ErrorCode;

/**
 * 自定义异常类
    自定义异常码和异常信息，方便去返回对应的/或者开发者自己的异常码和异常信息
 */
public class BusinessException extends RuntimeException {

    /**
     * 错误码
     */
    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    public int getCode() {
        return code;
    }
}
