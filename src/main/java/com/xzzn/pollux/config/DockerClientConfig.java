package com.xzzn.pollux.config;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Info;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.Duration;

@Slf4j
@Component
public class DockerClientConfig {

    @Value("${docker.host}")
    private String dockerHost;

    @Value("${docker.api-version}")
    private String dockerApiVersion;

    @Resource
    private ObjectMapper objectMapper;

    @Bean
    public DockerClient dockerClient() throws JsonProcessingException {
        DefaultDockerClientConfig config = DefaultDockerClientConfig
                .createDefaultConfigBuilder()
                //.withDockerTlsVerify(DOCKER_TLS_VERIFY)
                .withDockerHost(dockerHost)
                // 与docker版本对应，参考https://docs.docker.com/engine/api/#api-version-matrix
                // 或者通过docker version指令查看api version
                .withApiVersion(dockerApiVersion)
                //.withRegistryUrl(DOCKER_REGISTRY_URL)
                .build();

        // 创建DockerHttpClient
        DockerHttpClient httpClient = new ApacheDockerHttpClient
                .Builder()
                .dockerHost(config.getDockerHost())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofSeconds(45))
                .build();

        DockerClient dockerClient = DockerClientImpl.getInstance(config, httpClient);
        Info info = dockerClient.infoCmd().exec();
        String infoStr = objectMapper.writeValueAsString(info);
        log.debug("Docker Client Info: {}", infoStr);

        return dockerClient;
    }
}
