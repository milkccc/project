package com.xzzn.pollux.model.vo.request.task;

import lombok.Data;

import java.util.List;

@Data
public class QATaskDeleteProblemRequest {

    private String taskId;

    private List<String> fileIdList;
}
