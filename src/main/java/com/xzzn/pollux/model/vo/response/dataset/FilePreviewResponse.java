package com.xzzn.pollux.model.vo.response.dataset;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class FilePreviewResponse implements Serializable {

    private String fileId;
    private String fileName;
    private String fileType;
    private String srcFilePath;
    private String parseFilePath;
}
