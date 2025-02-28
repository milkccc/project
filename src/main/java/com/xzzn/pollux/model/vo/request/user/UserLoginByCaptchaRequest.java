package com.xzzn.pollux.model.vo.request.user;

import com.xzzn.pollux.model.pojo.FingerPrint;
import com.xzzn.pollux.common.enums.LoginTypeEnum;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * @author xzzn
 */
@Data
public class UserLoginByCaptchaRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "账号不能为空")
    private String account;

    @NotNull(message = "验证码不能为空")
    private int verificationCode;

    @NotNull(message = "登陆类型不能为空")
    private LoginTypeEnum loginType;

    @NotNull(message = "源信息不能为空")
    private FingerPrint fingerPrint;
}
