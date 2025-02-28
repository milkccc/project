package com.xzzn.pollux.model.vo.request.user;

import com.xzzn.pollux.model.pojo.FingerPrint;
import com.xzzn.pollux.common.enums.RegisterTypeEnum;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 用户注册请求体
 *
 * @author xzzn
 */
@Data
public class UserRegisterRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "用户名不能为空")
    private String userName;

    @NotNull(message = "账号不能为空")
    private String account;

    @NotNull(message = "密码不能为空")
    private String password;

    @NotNull(message = "验证码不能为空")
    private int verificationCode;

    @NotNull(message = "注册类型不能为空")
    private RegisterTypeEnum registerType;

    @NotNull(message = "源信息不能为空")
    private FingerPrint fingerPrint;

}
