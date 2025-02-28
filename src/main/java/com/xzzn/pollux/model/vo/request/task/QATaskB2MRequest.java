package com.xzzn.pollux.model.vo.request.task;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QATaskB2MRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String taskId;

    private String fileId;

    private String path;

    private Integer densityLevel;

    private Integer splitLevel;

    private String description;

    private String domain;

    private Integer priority;  //是否是生成小样的请求
}
