package com.xzzn.pollux.entity.es;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;

import java.io.Serializable;

@Data
@Builder
@Document(indexName = "file_content")
public class FileContent implements Serializable {
    @Id
    private String id;

    @Field(name = "content")
    private String content;

}