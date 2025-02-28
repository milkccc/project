package com.xzzn.pollux.utils;

import com.xzzn.pollux.common.ResultResponse;
import com.xzzn.pollux.common.enums.ErrorCodeEnum;

import java.io.Serializable;

public class ResultUtils {

    private ResultUtils() {
    }

    public static <T extends Serializable> ResultResponse<T> success(T data) {
        return new ResultResponse<>(200, "success", data);
    }

    public static <T extends Serializable> ResultResponse<T> error(int code, String message) {
        return new ResultResponse<>(code, message, null);
    }

    public static <T extends Serializable> ResultResponse<T> error(ErrorCodeEnum errorCodeEnum, String message) {
        return new ResultResponse<>(errorCodeEnum.getCode(), errorCodeEnum.getMessage() + message, null);
    }
}
