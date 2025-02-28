package com.xzzn.pollux.model.vo.request.qa;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.xzzn.pollux.entity.es.QADocument;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QAData {

    @JsonProperty("reference")
    private String reference;

    @JsonProperty("qa_pair_list")
    private List<QAPair> qaPairList;

    @JsonProperty("tag")
    private String tag;

    @JsonProperty("question")
    private String question;

    @JsonProperty("answer")
    private String answer;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QAPair {
        @JsonProperty("question")
        private String question;

        @JsonProperty("answer")
        private String answer;

        private List<QADocument.HighlightIdx> highlightIdxList;
    }
}
