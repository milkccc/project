package com.xzzn.pollux.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 用户登陆
 * </p>
 *
 * @author xzzn
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value="UserLogin对象", description="用户登陆")
@Builder
public class UserLogin implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "用户登陆id")
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    @ApiModelProperty(value = "用户ID")
    private String userId;

    @ApiModelProperty(value = "登陆时间")
    private LocalDateTime loginTime;

    @ApiModelProperty(value = "IP")
    private String loginIp;

    @ApiModelProperty(value = "设备")
    private String loginDevice;

    @ApiModelProperty(value = "操作系统")
    private String loginOs;

    @ApiModelProperty(value = "浏览器")
    private String loginBrowser;

    @ApiModelProperty(value = "登录状态 0-未登录 1-已登录")
    private Integer loginStatus;

    @ApiModelProperty(value = "登陆失败原因")
    private String loginFailReason;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updateTime;


}
