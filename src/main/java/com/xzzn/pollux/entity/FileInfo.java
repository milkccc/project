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
 * 文件信息表
 * </p>
 *
 * @author xzzn
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value="FileInfo对象", description="文件信息表")
@Builder
public class FileInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "文件id")
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    @ApiModelProperty(value = "数据集id")
    private String datasetId;

    @ApiModelProperty(value = "文件层级")
    private Integer fileLevel;

    @ApiModelProperty(value = "文件名称")
    private String fileName;

    @ApiModelProperty(value = "文件路径")
    private String filePath;

    @ApiModelProperty(value = "解析文件路径")
    private String parseFilePath;

    @ApiModelProperty(value = "文件类型")
    private String fileType;

    @ApiModelProperty(value = "文件大小")
    private Long fileSize;

    @ApiModelProperty(value = "文件状态")
    private String fileStatus;

    @ApiModelProperty(value = "解析失败原因")
    private String failReason;

    @ApiModelProperty(value = "上传时间")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updateTime;


}
