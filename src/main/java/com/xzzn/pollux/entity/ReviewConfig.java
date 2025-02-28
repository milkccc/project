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
 * 任务审核配置表
 * </p>
 *
 * @author xzzn
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value="ReviewConfig对象", description="任务审核配置表")
@Builder
public class ReviewConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "id")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty(value = "任务id")
    private String taskId;

    @ApiModelProperty(value = "问题审核标准")
    @TableField("Q_review_criteria")
    private String qReviewCriteria;

    @ApiModelProperty(value = "答案审核标准")
    @TableField("A_review_criteria")
    private String aReviewCriteria;

    @ApiModelProperty(value = "是否进行第二步审核")
    private Boolean isStepTwo;

    @ApiModelProperty(value = "打分审核标准")
    private String scoreReviewCriteria;

    @ApiModelProperty(value = "打分按钮信息")
    private String scoreButtonInfo;

    @ApiModelProperty(value = "上传时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:SS")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:SS")
    private LocalDateTime updateTime;


}
