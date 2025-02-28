package com.xzzn.pollux.model.pojo;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class DockerProp {

    private String containerName;

    private String imageName;

    private String imageTag;

    // 端口绑定（宿主机：容器）
    private Map<Integer, Integer> portMap;

    private Map<String, String> pathMap;

    private List<String> env;

    // 挂载分卷
    private List<String> volumes;

    private String dockerfilePath;

    private String repository;

    private String tag;
}