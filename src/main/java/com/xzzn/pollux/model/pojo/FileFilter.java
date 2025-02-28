package com.xzzn.pollux.model.pojo;

import lombok.Data;

import java.util.List;

@Data
public class FileFilter {
    private String fileName;

    private List<String> tags;

    private Long startTime;

    private Long endTime;
}
