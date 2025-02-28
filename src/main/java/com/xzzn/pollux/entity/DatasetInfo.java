package com.xzzn.pollux.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 数据集信息表
 * </p>
 *
 * @author xzzn
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value="DatasetInfo对象", description="数据集信息表")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatasetInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "数据集id")
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    @ApiModelProperty(value = "数据集名称")
    private String datasetName;

    @ApiModelProperty(value = "数据集存储路径")
    private String datasetPath;

    @ApiModelProperty(value = "数据集大小")
    private Long datasetSize;

    @ApiModelProperty(value = "数据集解析状态")
    private String datasetStatus;

    @ApiModelProperty(value = "上传用户id")
    private String uploaderId;

    @ApiModelProperty(value = "文件总数")
    private Integer total;

    @ApiModelProperty(value = "文件解析完成数")
    private Integer complete;

    @ApiModelProperty(value = "数据集结构树")
    private String tree;

    @ApiModelProperty(value = "标签")
    private String tags;

    @ApiModelProperty(value = "上传时间")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updateTime;


}
