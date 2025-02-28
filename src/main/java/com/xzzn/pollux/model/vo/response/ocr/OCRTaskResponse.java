package com.xzzn.pollux.model.vo.response.ocr;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class OCRTaskResponse {

    private String code;

    private String msg;

    private OCRTaskInfo data;

    @Data
    public static class OCRTaskInfo {
        @JsonProperty("file_id")
        private String fileId;
        @JsonProperty("file_name")
        private String fileName;
        @JsonProperty("target_file")
        private String targetFile;
        @JsonProperty("task_progress")
        private Double taskProgress;
        @JsonProperty("task_status")
        private String taskStatus;
    }
}
