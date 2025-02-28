package com.xzzn.pollux.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@TableName("page_hashes")
public class PageHash implements Serializable {

    @ApiModelProperty(value = "网址")
    @TableId(value = "url", type = IdType.INPUT)
    private String url;

    @TableField("content_hash")
    @ApiModelProperty(value = "内容哈希")
    private String contentHash;

    @TableField("last_updated")
    @ApiModelProperty(value = "上次更新时间")
    private LocalDateTime lastUpdated;
}
