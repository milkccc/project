package com.xzzn.pollux.common.exception;

import com.xzzn.pollux.common.enums.ErrorCodeEnum;

/**
 * 自定义异常类
 *
 * @author xzzn
 */
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(ErrorCodeEnum errorCodeEnum) {
        super(errorCodeEnum.getMessage());
        this.code = errorCodeEnum.getCode();
    }

    public BusinessException(ErrorCodeEnum errorCodeEnum, String message) {
        super(message);
        this.code = errorCodeEnum.getCode();
    }

    public int getCode() {
        return code;
    }
}
