package com.xzzn.pollux.model.vo.request.user;

import com.xzzn.pollux.model.pojo.FingerPrint;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 用户登录请求体
 *
 * @author xzzn
 */
@Data
public class UserLoginRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "账号不能为空")
    private String account;

    @NotNull(message = "密码不能为空")
    private String password;

    @NotNull(message = "源信息不能为空")
    private FingerPrint fingerPrint;
}
