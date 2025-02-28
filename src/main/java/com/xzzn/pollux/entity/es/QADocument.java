package com.xzzn.pollux.entity.es;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
//@Document(indexName = "#{@esUtils.getIndexName()}")
@Document(indexName = "qa_document_tt")
public class QADocument implements Serializable{
    @Id
    private String id;

    @Field(name = "question", type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_max_word")
    private String question;

    @Field(name = "answer", type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_max_word")
    private String answer;

    @Field("file_content_id")
    private String fileContentId;

    @Field(name = "file_id")
    private String fileId;

    @Field(name = "dataset_id")
    private String datasetId;

    @Field("is_allocated")
    private boolean isAllocated;

    @Field("allocated_user_id")
    private String allocatedUserId;

    @Field("is_review")
    private boolean isReview;

    @Field("is_modify")
    private String isModify;

    @Field("score")
    private String score;

    @Field("highlight_idx_list")
    private List<HighlightIdx> highlightIdxList;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HighlightIdx implements Serializable {
        private int start;
        private int end;
    }
}