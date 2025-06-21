package com.echo.virtual_interview.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用返回给前端的结果类
 *
 */
@Data
public class BaseResponse<T> implements Serializable {

    private int code;

    private T data;

    private String message;
    //1.传入code、数据、对应的信息进行返回

    public BaseResponse(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }
    //2.不需要传入信息
    public BaseResponse(int code, T data) {
        this(code, data, "");
    }
    //3.传入错误码，会自动包装为对应的错误码和其错误信息。
    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), null, errorCode.getMessage());
    }
}
