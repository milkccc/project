package com.xzzn.pollux.model.vo.response.dataset;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DatasetListResponse implements Serializable{

    private ArrayList<DatasetInfoVO> datasetInfoVOList;

    private Long total = 0L;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DatasetInfoVO implements Serializable {

        private String id;
        private String datasetName;
        private Long datasetSize;
        private String datasetStatus;
        private String uploaderId;
        private Integer total;
        private Integer complete;
        private List<String> tags;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:SS")
        private LocalDateTime createTime;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:SS")
        private LocalDateTime updateTime;
        private List<QATaskInfo> relatedQATaskList;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QATaskInfo implements Serializable{
        private String taskId;
        private String taskName;
    }
}
