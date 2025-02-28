package com.xzzn.pollux.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.model.*;
import com.google.common.collect.ImmutableSet;
import com.xzzn.pollux.common.exception.BusinessException;
import com.xzzn.pollux.model.pojo.DockerProp;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.io.InputStream;
import java.util.*;

import static com.github.dockerjava.api.model.HostConfig.newHostConfig;


@Service
public class DockerService {

    @Resource
    private DockerClient dockerClient;

    @Resource
    private ObjectMapper objectMapper;

    /**
     * 查看镜像详细信息
     *
     * @param imageName 镜像名称
     * @return 镜像详细信息
     */
    public String inspectImage(String imageName) {
        InspectImageResponse response = dockerClient.inspectImageCmd(imageName).exec();
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            throw new BusinessException(500, "");
        }
    }

    /**
     * 删除镜像
     *
     * @param imageName 镜像名称
     */
    public void removeImage(String imageName) {
        dockerClient.removeImageCmd(imageName).exec();
    }

    /**
     * 构建镜像
     *
     * @param dockerProp Docker配置
     * @return 构建镜像信息
     */
    public String buildImage(DockerProp dockerProp) {
        ImmutableSet<String> tag = ImmutableSet.of(dockerProp.getImageName() + ":" + dockerProp.getImageTag());
        return dockerClient.buildImageCmd(new File(dockerProp.getDockerfilePath()))
                .withTags(tag)
                .start()
                .awaitImageId();
    }

    /**
     * 给镜像打tag
     *
     * @param dockerProp Docker配置
     */
    public void tagImage(DockerProp dockerProp) {
        dockerClient.tagImageCmd(dockerProp.getImageName(), dockerProp.getRepository(), dockerProp.getTag()).exec();
    }

    /**
     * 加载镜像文件
     *
     * @param inputStream 输入流
     */
    public void loadImage(InputStream inputStream) {
        dockerClient.loadImageCmd(inputStream).exec();
    }

    /**
     * 获取镜像列表
     *
     * @return 镜像列表
     */
    public List<Image> imageList() {
        return dockerClient.listImagesCmd().withShowAll(true).exec();
    }

    /**
     * 创建容器
     *
     * @return 创建容器响应
     */
    public CreateContainerResponse createContainers(DockerProp dockerProp) {
        // 端口绑定
        Map<Integer, Integer> portMap = Optional.ofNullable(dockerProp).map(DockerProp::getPortMap).orElse(new HashMap<>());
        Iterator<Map.Entry<Integer, Integer>> iterator = portMap.entrySet().iterator();
        List<PortBinding> portBindingList = new ArrayList<>();
        List<ExposedPort> exposedPortList = new ArrayList<>();

        while (iterator.hasNext()) {
            Map.Entry<Integer, Integer> entry = iterator.next();
            ExposedPort tcp = ExposedPort.tcp(entry.getKey());
            Ports.Binding binding = Ports.Binding.bindPort(entry.getValue());
            PortBinding ports = new PortBinding(binding, tcp);
            portBindingList.add(ports);
            exposedPortList.add(tcp);
        }
        List<DeviceRequest> deviceRequests = new ArrayList<>();
        DeviceRequest deviceRequest = new DeviceRequest();
        List<List<String>> capabilities = new ArrayList<>();
        List<String> capability = new ArrayList<>();
        capability.add("gpu");
        capabilities.add(capability);
        deviceRequest.withCapabilities(capabilities).withCount(1).withDriver("nvidia");
        deviceRequests.add(deviceRequest);

        List<Bind> binds = new ArrayList<>();
        if (dockerProp != null) {
            for (String volumeStr : dockerProp.getVolumes()) {
                String[] split = volumeStr.split(":");
                Volume volume = new Volume(split[1]);
                Bind bind = new Bind(split[0], volume);
                binds.add(bind);
            }
        }

        return dockerClient.createContainerCmd(dockerProp.getImageName() + ":" + dockerProp.getImageTag())
                .withName(dockerProp.getContainerName())
                .withHostConfig(newHostConfig()
                        .withPortBindings(portBindingList)
                        .withDeviceRequests(deviceRequests)
                        .withBinds(binds))
                .withExposedPorts(exposedPortList)
                .withEnv(dockerProp.getEnv())
                .exec();

    }


    /**
     * 启动容器
     *
     * @param containerId 容器ID
     */
    public void startContainer(String containerId) {
        dockerClient.startContainerCmd(containerId).exec();
    }

    /**
     * 停止容器
     *
     * @param containerId 容器ID
     */
    public void stopContainer(String containerId) {
        dockerClient.stopContainerCmd(containerId).exec();
    }

    /**
     * 删除容器
     *
     * @param containerId 容器ID
     */
    public void removeContainer(String containerId) {
        dockerClient.removeContainerCmd(containerId).exec();
    }
}