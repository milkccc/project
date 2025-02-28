package com.xzzn.pollux.model.vo.response.task;

import com.xzzn.pollux.model.pojo.TaskConfigMap;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QATaskRetryResponse {

    private String taskName;

    private TaskConfigMap taskConfigMap;

    private String domain;

    private String description;

}
