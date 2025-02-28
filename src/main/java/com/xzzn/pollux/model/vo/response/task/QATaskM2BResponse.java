package com.xzzn.pollux.model.vo.response.task;


import com.xzzn.pollux.common.enums.QATaskM2BStatusEnum;
import com.xzzn.pollux.entity.es.QADocument;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QATaskM2BResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private QATaskM2BStatusEnum status;

    private String taskId;

    private String fileId;

    private Integer priority;

    private List<Chunk> chunkList;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Chunk implements Serializable{
        private String chunkContent;
        private List<QAPairWithHighLightIdx> qaPairList;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QAPairWithHighLightIdx implements Serializable{
        private String question;
        private String answer;
        private List<QADocument.HighlightIdx> highlightIdxList;
    }

}
