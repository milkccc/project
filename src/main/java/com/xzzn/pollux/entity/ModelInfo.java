package com.xzzn.pollux.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 模型信息表
 * </p>
 *
 * @author xzzn
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "ModelInfo对象", description = "模型信息表")
@Builder
public class ModelInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "模型id")
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    @ApiModelProperty(value = "模型名称")
    private String modelName;

    @ApiModelProperty(value = "模型种类,如基座模型,微调训练模型")
    private String modelCategory;

    @ApiModelProperty(value = "模型类型,如文生图类型")
    private String modelType;

    @ApiModelProperty(value = "模型简介")
    private String modelIntro;

    @ApiModelProperty(value = "模型场景")
    private String modelScene;

    @ApiModelProperty(value = "模型参数")
    private String modelParam;

    @ApiModelProperty(value = "模型存放路径")
    private String modelPath;

    @ApiModelProperty(value = "基座模型")
    private String baseModel;

    @ApiModelProperty(value = "微调训练用户")
    private String userId;

    @ApiModelProperty(value = "训练测试集比例")
    private Double testSetRatio;

    @ApiModelProperty(value = "训练策略")
    private String trainStrategy;

    @ApiModelProperty(value = "迭代轮次")
    private Integer iterationRound;

    @ApiModelProperty(value = "学习率")
    private Double learningRate;

    @ApiModelProperty(value = "批次大小")
    private Integer batchSize;

    @ApiModelProperty(value = "训练状态")
    private String trainStatus;

    @ApiModelProperty(value = "训练日志")
    private String trainLog;

    @ApiModelProperty(value = "中断原因")
    private String stopReason;

    @ApiModelProperty(value = "评估状态")
    private String evaluateStatus;

    @ApiModelProperty(value = "评估分数")
    private String evaluateScore;

    @ApiModelProperty(value = "上次上线时间")
    private LocalDateTime lastOnlineTime;

    @ApiModelProperty(value = "上线时间")
    private LocalDateTime onlineTime;

    @ApiModelProperty(value = "上线次数")
    private Integer onlineCount;

    @ApiModelProperty(value = "上线持续时间 ")
    private String duration;

    @ApiModelProperty(value = "发布参数配置")
    private String publishConfig;

    @ApiModelProperty(value = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:SS")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:SS")
    private LocalDateTime updateTime;


}
