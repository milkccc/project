package com.xzzn.pollux.model.pojo;

import lombok.Data;

@Data
public class QATaskFilter {
    // 任务名称
    private String taskName;
    // 任务状态
    private String taskStatus;
    // 任务类型
    private String taskType;
    // 任务创建者
    private String taskCreator;
    // 任务开始时间
    private Long createTime;
    // 任务结束时间
    private Long endTime;
}
