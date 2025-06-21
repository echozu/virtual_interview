package com.echo.virtual_interview.common;

/**
 * 返回工具类【利用了BaseResPonse】
 *
 */
public class ResultUtils {

    /**
     * 成功
     *使用：1.返回参数写 BaseResponse<T>
     * 2.return ResultUtils.success(data)
     * @param data
     * @param <T>
     * @return
     */
    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(0, data, "ok");
    }

    /**
     * 失败
     *使用：return ResultUtils.error(错误码，如Error.PARAMS_ERROR)
     * @param errorCode
     * @return
     */
    public static <T> BaseResponse <T> error(ErrorCode errorCode) {
        return new BaseResponse<>(errorCode);
    }

    /**
     * 失败
     *使用：return ResultUtils.error(code,错误信息)
     * @param code
     * @param message
     * @return
     */
    public static  <T>BaseResponse <T> error(int code, String message) {
        return new BaseResponse(code, null, message);
    }

    /**
     * 失败
     *使用：return ResultUtils.error(错误码，如Error.PARAMS_ERROR，错误信息)
     * @param errorCode
     * @return
     */
    public static  <T>BaseResponse <T> error(ErrorCode errorCode, String message) {
        return new BaseResponse<>(errorCode.getCode(), null, message);
    }
}
