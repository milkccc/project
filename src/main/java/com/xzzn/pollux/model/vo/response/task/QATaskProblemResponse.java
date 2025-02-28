package com.xzzn.pollux.model.vo.response.task;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QATaskProblemResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;

    private String taskId;

    private String fileId;

    private String status;

    private Integer qaCount;

    private String fileName;

}
