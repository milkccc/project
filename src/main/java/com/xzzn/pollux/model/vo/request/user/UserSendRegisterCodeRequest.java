package com.xzzn.pollux.model.vo.request.user;

import com.xzzn.pollux.model.pojo.FingerPrint;
import com.xzzn.pollux.common.enums.RegisterTypeEnum;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * @author xzzn
 * @date 2023/10/18
 */
@Data
public class UserSendRegisterCodeRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "账号不能为空")
    private String account;

    @NotNull(message = "注册类型不能为空")
    private RegisterTypeEnum registerType;

    @NotNull(message = "源信息不能为空")
    private FingerPrint fingerPrint;
}
