package com.echo.virtual_interview.common;

import lombok.extern.slf4j.Slf4j;

/**
 * 返回工具类【利用了BaseResponse】
 * 使用清晰格式的日志输出，不依赖ANSI颜色
 */
@Slf4j
public class ResultUtils {

    /**
     * 成功响应
     * 使用示例: return ResultUtils.success(data);
     *
     * @param data 返回数据
     * @param <T>  数据类型
     * @return 成功响应
     */
    public static <T> BaseResponse<T> success(T data) {
        BaseResponse<T> response = new BaseResponse<>(0, data, "ok");
        logResponse("SUCCESS", response);
        return response;
    }

    /**
     * 失败响应（基于错误码）
     * 使用示例: return ResultUtils.error(Error.PARAMS_ERROR);
     *
     * @param errorCode 错误码
     * @return 错误响应
     */
    public static <T> BaseResponse<T> error(ErrorCode errorCode) {
        BaseResponse<T> response = new BaseResponse<>(errorCode);
        logResponse("ERROR", response);
        return response;
    }

    /**
     * 失败响应（自定义状态码+消息）
     * 使用示例: return ResultUtils.error(400, "参数错误");
     *
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
     * 失败响应（错误码+自定义信息）
     * 使用示例: return ResultUtils.error(ErrorCode.PARAMS_ERROR, "参数为空");
     *
     * @param errorCode 错误码对象
     * @param message 自定义错误信息
     * @return 错误响应
     */
    public static <T> BaseResponse<T> error(ErrorCode errorCode, String message) {
        BaseResponse<T> response = new BaseResponse<>(errorCode.getCode(), null, message);
        logResponse("ERROR", response);
        return response;
    }

    /**
     * 日志记录响应信息（使用分隔线代替颜色）
     * @param type 响应类型（SUCCESS / ERROR）
     * @param response 响应对象
     */
    private static void logResponse(String type, BaseResponse<?> response) {
        String border = "==================================================";
        String header = String.format("%s\n[%s] 返回给前端的响应\n%s", border, type, border);

        String body = String.format(
                "\nCode: %d\n" +
                        "Message: %s\n" +
                        "Data: %s\n" +
                        "%s",
                response.getCode(),
                response.getMessage(),
                response.getData(),
                border);

        String fullLog = header + body;

        if ("ERROR".equals(type)) {
            log.error(fullLog);
        } else {
            log.info(fullLog);
        }
    }
}