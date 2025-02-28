package com.xzzn.pollux.model.vo.request.task;

import com.xzzn.pollux.model.pojo.TaskConfigMap;
import lombok.Data;

import java.util.List;

@Data
public class QATaskCreateRequest {
    private String taskName;

    private List<String> datasetList;

    private TaskConfigMap taskConfigMap;

    private String domain;

    private String description;

    private boolean priority;  //是否生成小样QA对

    public QATaskCreateRequest() {

        this.description = "";

        this.priority = false;
    }
}
