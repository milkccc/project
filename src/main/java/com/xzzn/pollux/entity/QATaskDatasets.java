package com.xzzn.pollux.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * <p>
 * 任务数据集关联表
 * </p>
 *
 * @author xzzn
 */
@TableName("qa_task_datasets")
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value="QATaskDatasets对象", description="任务数据集关联表")
public class QATaskDatasets implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "id")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty(value = "任务id")
    private String taskId;

    @ApiModelProperty(value = "数据集id")
    private String datasetId;

    @ApiModelProperty(value = "任务数据集状态")
    private String status;


}
