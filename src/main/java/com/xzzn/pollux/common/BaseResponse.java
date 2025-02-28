package com.xzzn.pollux.common;

import com.xzzn.pollux.common.enums.ErrorCodeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 通用返回类
 *
 * @author xzzn
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaseResponse implements Serializable {

    private int code;

    private String message;

    public BaseResponse(ErrorCodeEnum errorCodeEnum) {
        this(errorCodeEnum.getCode(), errorCodeEnum.getMessage());
    }

    public static BaseResponse success() {
        return new BaseResponse(200, "success");
    }

    public static BaseResponse error() {
        return new BaseResponse(500, "falied");
    }
    public static BaseResponse error(String message) {
        return new BaseResponse(500, message);
    }

}
