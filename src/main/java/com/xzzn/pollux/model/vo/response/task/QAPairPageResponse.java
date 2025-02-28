package com.xzzn.pollux.model.vo.response.task;

import com.xzzn.pollux.entity.es.QADocument;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QAPairPageResponse implements Serializable {
    private List<QADocument> qaDocumentPage;
    private Long total;
}
