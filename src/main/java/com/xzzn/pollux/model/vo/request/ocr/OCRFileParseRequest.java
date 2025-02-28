package com.xzzn.pollux.model.vo.request.ocr;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
public class OCRFileParseRequest {

    private String datasetId;

    private String fileId;

    private String pathSuffix;

    private String filePath;

    private String fileName;   // new add

    @JsonCreator
    public OCRFileParseRequest(@JsonProperty("datasetId") String datasetId,
                               @JsonProperty("fileId") String fileId,
                               @JsonProperty("pathSuffix") String pathSuffix,
                               @JsonProperty("filePath") String filePath,
                                @JsonProperty("fileName") String fileName){
        this.datasetId = datasetId;
        this.fileId = fileId;
        this.pathSuffix = pathSuffix;
        this.filePath = filePath;
        this.fileName = fileName;        // new add
    }

}
