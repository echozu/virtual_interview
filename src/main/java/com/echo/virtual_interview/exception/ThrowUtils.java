package com.echo.virtual_interview.exception;


import com.echo.virtual_interview.common.ErrorCode;

/**
 * 抛异常工具类
 *这里的作用类似：if(username==null){new throw xxException}
 * 这里就是简化为直接调这个即可抛出异常
 */
public class ThrowUtils {
    /**
     * 使用：
     * ThrowUtils.throwIf(auth,ErrorCode.NO_AUTH_ERROR,"没权限")  即如果这里auth为空的话就会抛出异常：403，没权限
     * 相当于：
     * if(auth){
     *     new throw RunTimeException("没权限")
     * }
     */
    /**
     * 1.条件成立则抛异常
     *传入判断条件，抛出runtimeException
     * @param condition
     * @param runtimeException
     */
    public static void throwIf(boolean condition, RuntimeException runtimeException) {
        if (condition) {
            throw runtimeException;
        }
    }

    /**
     * 2.条件成立则抛异常
     *传入要判断的条件和异常码，方便去抛出对应的异常码和异常信息。
     * @param condition
     * @param errorCode
     */
    public static void throwIf(boolean condition, ErrorCode errorCode) {
        throwIf(condition, new BusinessException(errorCode));
    }

    /**
     * 3.条件成立则抛异常
     *传入判断条件、错误码、如果报错了要抛出的提醒异常信息 从而去抛异常。
     * @param condition
     * @param errorCode
     * @param message
     */
    public static void throwIf(boolean condition, ErrorCode errorCode, String message) {
        throwIf(condition, new BusinessException(errorCode, message));
    }

}
