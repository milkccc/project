package com.xzzn.pollux.model.vo.response.task;

import com.xzzn.pollux.model.pojo.FileTreeNodeForProblem;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
public class QATaskProblemTreeResponse implements Serializable {

    private String taskId;

    private List<QATaskProblem> problems;

    @Data
    @AllArgsConstructor
    public static class QATaskProblem implements Serializable{
        private String datasetId;

        private FileTreeNodeForProblem fileTreeNodeForProblem;
    }
}
