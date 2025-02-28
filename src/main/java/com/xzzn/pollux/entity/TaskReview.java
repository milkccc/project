package com.xzzn.pollux.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
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
 * 任务审核表
 * </p>
 *
 * @author xzzn
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value="TaskReview对象", description="任务审核表")
@Builder
public class TaskReview implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "id")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty(value = "用户id")
    private String userId;

    @ApiModelProperty(value = "任务id")
    private String taskId;

    @ApiModelProperty(value = "上一次分配的qa对所属子文件id")
    private String lastChildFileId;

    @ApiModelProperty(value = "当前分配的qa对id列表")
    @TableField("allocated_qa_list")
    private String allocatedQAList;

    @ApiModelProperty(value = "当前分配的qa对审核数")
    @TableField("allocated_qa_num")
    private Integer allocatedQANum;

    @ApiModelProperty(value = "当前批次审核数")
    private Integer reviewCurNum;

    @ApiModelProperty(value = "总审核数")
    private Integer reviewTotalNum;

    @ApiModelProperty(value = "上传时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:SS")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:SS")
    private LocalDateTime updateTime;


}
