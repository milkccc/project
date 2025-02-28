package com.xzzn.pollux.model.pojo;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

@Data
public class FingerPrint implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "IP不能为空")
    private String ip;

    @NotBlank(message = "设备不能为空")
    private String device;

    @NotBlank(message = "浏览器不能为空")
    private String browser;

    @NotBlank(message = "操作系统不能为空")
    private String os;
}
