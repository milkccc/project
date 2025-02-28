package com.xzzn.pollux.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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
 * 推理任务表
 * </p>
 *
 * @author xzzn
 */
@TableName("qa_task")
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value="QATask对象", description="推理任务表")
@Builder
public class QATask implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "任务id")
    @TableId(value = "id")
    private String id;

    @ApiModelProperty(value = "任务名称")
    private String taskName;

    @ApiModelProperty(value = "段落精细度")
    private Integer splitLevel;

    @ApiModelProperty(value = "提问密度")
    private Integer densityLevel;

    @ApiModelProperty(value = "提问领域")
    private String domain;

    @ApiModelProperty(value = "需求描述")
    private String description;

    @ApiModelProperty(value = "任务状态")
    private String taskStatus;

    @ApiModelProperty(value = "任务开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:SS")
    private LocalDateTime taskStartTime;

    @ApiModelProperty(value = "任务结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:SS")
    private LocalDateTime taskEndTime;

    @ApiModelProperty(value = "任务创建者id")
    private String taskCreatorId;

    @ApiModelProperty(value = "文件总数")
    private Integer total;

    @ApiModelProperty(value = "文件已完成数")
    private Integer complete;

    @ApiModelProperty(value = "生成QA对数量")
    private Integer qaCount;

    @ApiModelProperty(value = "审核QA对数量")
    private Integer reviewCount;

    @ApiModelProperty(value = "审核配置id,为null则说明未设置")
    private Integer reviewConfigId;

    @ApiModelProperty(value = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:SS")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:SS")
    private LocalDateTime updateTime;
}
