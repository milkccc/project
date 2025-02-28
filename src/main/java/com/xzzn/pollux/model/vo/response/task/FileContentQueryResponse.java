package com.xzzn.pollux.model.vo.response.task;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class FileContentQueryResponse implements Serializable {

    private String id;

    private String content;

    private String fileId;

    private String fileName;
}
