package com.xzzn.pollux.model.vo.response.task;

import com.xzzn.pollux.model.pojo.FileTreeNodeForTask;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class QaTaskTreeResponse implements Serializable {

    private String datasetId;

    private FileTreeNodeForTask fileTreeNodeForTask;
}
