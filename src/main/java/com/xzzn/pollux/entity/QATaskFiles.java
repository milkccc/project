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
 * 任务文件关联表
 * </p>
 *
 * @author xzzn
 */
@Data
@TableName("qa_task_files")
@EqualsAndHashCode(callSuper = false)
@ApiModel(value="QATaskFiles对象", description="任务文件关联表")
public class QATaskFiles implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "id")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty(value = "任务id")
    private String taskId;

    @ApiModelProperty(value = "子文件id")
    private String fileId;

    @ApiModelProperty(value = "任务文件状态")
    private String status;

    @ApiModelProperty(value = "QA对数量")
    private Integer qaCount;

}
