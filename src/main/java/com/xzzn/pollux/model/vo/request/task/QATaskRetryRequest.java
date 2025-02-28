package com.xzzn.pollux.model.vo.request.task;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class QATaskRetryRequest {

    @NotBlank
    private String taskId;

    @NotNull
    private List<String> fileIdList;
}
