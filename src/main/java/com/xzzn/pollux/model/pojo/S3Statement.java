package com.xzzn.pollux.model.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class S3Statement {
    private List<String> action;
    private String effect;
    private Map<String, Object> principal;
    private List<String> resource;

}