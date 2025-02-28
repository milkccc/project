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
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * <p>
 * 活跃度信息表
 * </p>
 *
 * @author xzzn
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value="ActiveInfo对象", description="活跃度信息表")
@Builder
public class ActiveInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "id")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty(value = "日期")
    private LocalDate dateTime;

    @ApiModelProperty(value = "用户日活")
    private Long userActive;

    @ApiModelProperty(value = "数据生成日活")
    private Long dataGenerate;

    @ApiModelProperty(value = "数据审核日活")
    private Long dataReview;

    @ApiModelProperty(value = "上传时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:SS")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:SS")
    private LocalDateTime updateTime;


}
