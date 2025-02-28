package com.xzzn.pollux.model.vo.request.dataset;

import lombok.Data;

import java.util.List;
@Data
public class DatasetChangeTagsRequest {

    String datasetId;

    private List<String> tags;
}
