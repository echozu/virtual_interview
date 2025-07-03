package com.echo.virtual_interview.common;

import lombok.extern.slf4j.Slf4j;

/**
 * 返回工具类【利用了BaseResPonse】
 * 添加了彩色日志输出功能
 */
@Slf4j
public class ResultUtils {

    // ANSI color codes
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_CYAN = "\u001B[36m";

    /**
     * 成功
     * 使用：1.返回参数写 BaseResponse<T>
     * 2.return ResultUtils.success(data)
     * @param data 返回数据
     * @param <T> 数据类型
     * @return 成功响应
     */
    public static <T> BaseResponse<T> success(T data) {
        BaseResponse<T> response = new BaseResponse<>(0, data, "ok");
        logResponse("SUCCESS", response);
        return response;
    }

    /**
     * 失败
     * 使用：return ResultUtils.error(错误码，如Error.PARAMS_ERROR)
     * @param errorCode 错误码
     * @return 错误响应
     */
    public static <T> BaseResponse<T> error(ErrorCode errorCode) {
        BaseResponse<T> response = new BaseResponse<>(errorCode);
        logResponse("ERROR", response);
        return response;
    }

    /**
     * 失败
     * 使用：return ResultUtils.error(code,错误信息)
     * @param code 错误码
     * @param message 错误信息
     * @return 错误响应
     */
    public static <T> BaseResponse<T> error(int code, String message) {
        BaseResponse<T> response = new BaseResponse(code, null, message);
        logResponse("ERROR", response);
        return response;
    }

    /**
     * 失败
     * 使用：return ResultUtils.error(错误码，如Error.PARAMS_ERROR，错误信息)
     * @param errorCode 错误码
     * @param message 自定义错误信息
     * @return 错误响应
     */
    public static <T> BaseResponse<T> error(ErrorCode errorCode, String message) {
        BaseResponse<T> response = new BaseResponse<>(errorCode.getCode(), null, message);
        logResponse("ERROR", response);
        return response;
    }

    /**
     * 日志记录响应信息（带颜色）
     * @param type 响应类型
     * @param response 响应对象
     */
    private static void logResponse(String type, BaseResponse<?> response) {
        String color = ANSI_GREEN;
        String level = "INFO";

        if ("ERROR".equals(type)) {
            color = ANSI_RED;
            level = "ERROR";
        }

        String logMessage = String.format("\n%s[%s] 返回给前端的响应%s\n" +
                        "%sCode: %s%d%s\n" +
                        "%sMessage: %s%s%s\n" +
                        "%sData: %s%s%s",
                color, type, ANSI_RESET,
                ANSI_CYAN, color, response.getCode(), ANSI_RESET,
                ANSI_CYAN, color, response.getMessage(), ANSI_RESET,
                ANSI_CYAN, color, response.getData(), ANSI_RESET);

        if ("ERROR".equals(type)) {
            log.error(logMessage);
        } else {
            log.info(logMessage);
        }
    }
}