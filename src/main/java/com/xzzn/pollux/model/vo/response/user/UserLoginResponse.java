package com.xzzn.pollux.model.vo.response.user;

import lombok.Data;

import java.io.Serializable;


/**
 * 登陆返回类
 *
 * @author xzzn
 */
@Data
public class UserLoginResponse implements Serializable {

    private int code;

    private String token;

    private String message;

    public UserLoginResponse(int code, String token, String message) {
        this.code = code;
        this.token = token;
        this.message = message;
    }
}
