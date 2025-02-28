package com.xzzn.pollux.common;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 结果返回类
 *
 * @param <T>
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ResultResponse<T extends Serializable> extends BaseResponse {

    private T data;

    public ResultResponse(int code, String message, T data) {
        super(code, message);
        this.data = data;
    }

}
