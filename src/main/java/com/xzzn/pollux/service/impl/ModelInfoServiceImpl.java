package com.xzzn.pollux.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.xzzn.pollux.common.enums.ModelCategoryEnum;
import com.xzzn.pollux.common.enums.ModelTrainStatusEnum;
import com.xzzn.pollux.common.exception.BusinessException;
import com.xzzn.pollux.entity.ModelInfo;
import com.xzzn.pollux.mapper.ModelInfoMapper;
import com.xzzn.pollux.model.pojo.DockerProp;
import com.xzzn.pollux.model.pojo.ModelTrainLog;
import com.xzzn.pollux.model.pojo.ModelTrainParam;
import com.xzzn.pollux.model.pojo.QAPair;
import com.xzzn.pollux.model.vo.response.model.ModelListResponse;
import com.xzzn.pollux.service.DockerService;
import com.xzzn.pollux.service.IModelInfoService;
import com.xzzn.pollux.service.QAService;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * <p>
 * 模型信息表 服务实现类
 * </p>
 *
 * @author xzzn
 */
@Service
public class ModelInfoServiceImpl extends ServiceImpl<ModelInfoMapper, ModelInfo> implements IModelInfoService {

    @Value("${fine-tune-train.env.task-name}")
    private String fineTuneTrainEnvTaskName;

    @Value("${fine-tune-train.env.model-path-name}")
    private String fineTuneTrainEnvModelPathName;

    @Value("${fine-tune-train.model.host-dir}")
    private String fineTuneTrainModelHostDir;

    @Value("${fine-tune-train.data.volume}")
    private String fineTuneTrainDataVolume;

    @Resource
    private ScheduledExecutorService trainProgressExecutor;

    @Resource
    private QAService qaService;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private DockerService dockerService;


    @Override
    public ModelInfo getModel(String userId, String modelId) {
        ModelInfo modelInfo = existsModel(modelId);
        if (!Objects.equals(modelInfo.getModelCategory(), ModelCategoryEnum.BASE_MODEL.name())
                && !Objects.equals(modelInfo.getUserId(), userId)) {
            throw new BusinessException(500, "该用户无查看该模型的权限");
        }
        String baseModelId = modelInfo.getBaseModel();
        ModelInfo baseModelInfo = existsModel(baseModelId);
        baseModelInfo.setBaseModel(baseModelInfo.getModelName());
        return modelInfo;
    }

    @Override
    public ModelListResponse getModelInfoList(Integer page, Integer size, String sortAttribute, String sortDirection,
                                              String modelName, String modelStatus, String modelCategory, String userId) {
        QueryWrapper<ModelInfo> queryWrapper = buildQueryWrapper(sortAttribute, sortDirection, modelName, modelStatus,
                modelCategory, userId);

        // 返回查询结果总数
        long totalCount = this.count(queryWrapper);

        // 根据page和size进行分页
        List<ModelInfo> modelInfoList = this.page(new Page<>(page, size), queryWrapper).getRecords();
        return generateModelListResponse(modelInfoList, totalCount);
    }

    @Override
    public void deleteModels(List<String> modelIds) {
        if (modelIds.isEmpty()) {
            return;
        }
        this.baseMapper.deleteBatchIds(modelIds);
    }

    @Override
    public String addBaseModel(String modelName, String modelIntro, String modelType, String modelParam, String modelScene) {
        ModelInfo model = ModelInfo.builder()
                .modelName(modelName)
                .modelIntro(modelIntro)
                .modelType(modelType)
                .modelParam(modelParam)
                .modelScene(modelScene)
                .modelCategory(ModelCategoryEnum.BASE_MODEL.name())
                .build();
        boolean save = this.save(model);
        if (!save) {
            throw new BusinessException(500, "保存基座模型信息失败");
        }
        return model.getId();
    }

    @Override
    public void updateModel(String modelId, String modelName, String modelIntro, String modelScene) {
        ModelInfo modelInfo = existsModel(modelId);

        Optional.ofNullable(modelName).ifPresent(modelInfo::setModelName);
        Optional.ofNullable(modelIntro).ifPresent(modelInfo::setModelIntro);
        Optional.ofNullable(modelScene).ifPresent(modelInfo::setModelScene);

        this.updateById(modelInfo);
    }

    @Override
    public void startModelFineTuning(String baseModelId, String modelName, String modelIntro, List<String> taskIdList,
                                     Double testSetRatio, String trainStrategy, Integer iterationRound, Double learningRate,
                                     Integer batchSize, String userId) {
        // 获取基座模型信息
        ModelInfo baseModelInfo = existsModel(baseModelId);
        String modelPath = baseModelInfo.getModelPath();
        // 保存微调训练模型信息
        ModelInfo modelInfo = ModelInfo.builder()
                .modelName(modelName)
                .modelCategory(ModelCategoryEnum.FINE_TUNING_MODEL.name())
                .modelType(baseModelInfo.getModelType())
                .modelIntro(modelIntro)
                .modelScene(baseModelInfo.getModelScene())
                .modelParam(baseModelInfo.getModelParam())
                .baseModel(baseModelInfo.getId())
                .userId(userId)
                .testSetRatio(testSetRatio)
                .trainStrategy(trainStrategy)
                .iterationRound(iterationRound)
                .learningRate(learningRate)
                .batchSize(batchSize)
                .trainStatus(ModelTrainStatusEnum.UNTRAINED.name())
                .build();
        this.save(modelInfo);
        // 获取数据集,持久化到本地
        List<QAPair> qaPairs = new ArrayList<>();
        for (String taskId : taskIdList) {
            qaPairs.addAll(qaService.getQAPairs(true, null, null, null, taskId));
        }
        String trainSetAsJSON;
        try {
            trainSetAsJSON = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(qaPairs);
        } catch (JsonProcessingException e) {
            throw new BusinessException(500, "生成训练集失败: 训练集QA序列化失败-" + e.getMessage());
        }
        // 指定要写入的文件路径
        File file = new File("/data/pollux/finetune/" + modelInfo.getId() + "/train_set.json");
        file.getParentFile().mkdirs();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(trainSetAsJSON);
        } catch (IOException e) {
            throw new BusinessException(500, "生成训练集失败: 写入训练集文件失败-" + e.getMessage());
        }

        ModelTrainParam modelTrainParam = ModelTrainParam.builder()
                .baseModel(baseModelInfo.getModelName())
                .testSetRatio(testSetRatio)
                .trainStrategy(trainStrategy)
                .iterationRound(iterationRound)
                .learningRate(learningRate)
                .batchSize(batchSize)
                .build();
        String modelTrainParamAsJSON;
        try {
            modelTrainParamAsJSON = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(modelTrainParam);
        } catch (JsonProcessingException e) {
            throw new BusinessException(500, "生成训练集失败: 训练参数序列化失败-" + e.getMessage());
        }
        // 指定要写入的文件路径
        File file2 = new File("/data/pollux/finetune/" + modelInfo.getId() + "/train_param.json");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file2))) {
            writer.write(modelTrainParamAsJSON);
        } catch (IOException e) {
            throw new BusinessException(500, "生成训练集失败: 写入训练参数文件失败-" + e.getMessage());
        }
        // 启动docker镜像 pollux-model 传入这些参数和持久化数据集的地址
        DockerProp dockerProp = buildDockerProp(modelInfo, modelPath);
        CreateContainerResponse container = dockerService.createContainers(dockerProp);
        if (container == null) {
            throw new BusinessException(500, "训练失败:训练模型启动失败");
        }
        dockerService.startContainer(container.getId());
        trainProgressExecutor.scheduleWithFixedDelay(() -> asyncListenTrainProgress(modelInfo.getId()), 5, 5, TimeUnit.SECONDS);
    }

    public void asyncListenTrainProgress(String modelId) {
        String filePath = "/data/pollux/finetune/" + modelId + "/trained_model/trainer_log.jsonl";
        String lastLine = null;

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String currentLine;
            while ((currentLine = reader.readLine()) != null) {
                lastLine = currentLine;
            }
        } catch (IOException e) {
            log.error("读取训练日志异常: ", e);
        }
        if (lastLine != null) {
            ModelTrainLog modelTrainLog = null;
            try {
                modelTrainLog = objectMapper.readValue(lastLine, ModelTrainLog.class);
            } catch (JsonProcessingException e) {
                log.error("训练进度转换为对象失败: ", e);
            }
            if (modelTrainLog != null) {
                this.lambdaUpdate().eq(ModelInfo::getId, modelId)
                        .set(ModelInfo::getTrainLog, modelTrainLog)
                        .update();
                log.debug(modelTrainLog.toString());
                if (modelTrainLog.getCurrentSteps() == modelTrainLog.getTotalSteps()) {
                    trainProgressExecutor.shutdown();
                    this.lambdaUpdate().eq(ModelInfo::getId, modelId)
                            .set(ModelInfo::getTrainStatus, ModelTrainStatusEnum.COMPLETED)
                            .update();
                }
            }
        }
    }

    @Override
    public String getTrainProgress(String modelId, String id) {
        ModelInfo modelInfo = existsModel(modelId);
        return modelInfo.getTrainLog();
    }


    private QueryWrapper<ModelInfo> buildQueryWrapper(String sortAttribute, String sortDirection, String modelName,
                                                      String modelStatus, String modelCategory, String userId) {
        QueryWrapper<ModelInfo> queryWrapper = new QueryWrapper<>();
        handleModelName(modelName, queryWrapper);
        handleModelStatus(modelStatus, queryWrapper);
        handleModelCategory(modelCategory, queryWrapper);
        handleSorting(sortAttribute, sortDirection, queryWrapper);
        if (!Objects.equals(modelCategory, ModelCategoryEnum.BASE_MODEL.name()) && StringUtils.isNotBlank(modelCategory)) {
            handleUserId(userId, queryWrapper);
        }
        return queryWrapper;
    }

    private void handleModelName(String modelName, QueryWrapper<ModelInfo> queryWrapper) {
        if (StringUtils.isNotBlank(modelName)) {
            queryWrapper.lambda().like(ModelInfo::getModelName, modelName);
        }
    }

    private void handleModelStatus(String modelStatus, QueryWrapper<ModelInfo> queryWrapper) {
        if (StringUtils.isNotBlank(modelStatus)) {
            queryWrapper.lambda().eq(ModelInfo::getTrainStatus, modelStatus);
        }
    }

    private void handleModelCategory(String modelCategory, QueryWrapper<ModelInfo> queryWrapper) {
        if (StringUtils.isNotBlank(modelCategory)) {
            queryWrapper.lambda().eq(ModelInfo::getModelCategory, modelCategory);
        }
    }

    private void handleSorting(String sortAttribute, String sortDirection, QueryWrapper<ModelInfo> queryWrapper) {
        if (StringUtils.isNotBlank(sortAttribute) && StringUtils.isNotBlank(sortDirection)) {
            if ("asc".equalsIgnoreCase(sortDirection)) {
                queryWrapper.orderByAsc(StringUtils.camelToUnderline(sortAttribute));
            } else if ("desc".equalsIgnoreCase(sortDirection)) {
                queryWrapper.orderByDesc(StringUtils.camelToUnderline(sortAttribute));
            }
        }
    }

    private void handleUserId(String userId, QueryWrapper<ModelInfo> queryWrapper) {
        if (StringUtils.isNotBlank(userId)) {
            queryWrapper.lambda().eq(ModelInfo::getUserId, userId);
        }
    }

    private ModelListResponse generateModelListResponse(List<ModelInfo> modelInfoList, long totalCount) {
        ArrayList<ModelListResponse.ModelInfoVO> modelInfoVOList = new ArrayList<>();
        modelInfoList.forEach(modelInfo -> {
            String modelId = modelInfo.getId();
            ModelListResponse.ModelInfoVO modelInfoVO = ModelListResponse.ModelInfoVO.builder()
                    .id(modelId)
                    .modelName(modelInfo.getModelName())
                    .modelStatus(modelInfo.getTrainStatus())
                    .modelIntro(modelInfo.getModelIntro())
                    .build();

            modelInfoVOList.add(modelInfoVO);
        });
        return ModelListResponse.builder()
                .modelInfoVOList(modelInfoVOList)
                .total(totalCount)
                .build();
    }

    private ModelInfo existsModel(String modelId) {
        ModelInfo modelInfo = this.getById(modelId);

        if (modelInfo == null) {
            throw new BusinessException(500, "模型不存在");
        }
        return modelInfo;
    }

    private DockerProp buildDockerProp(ModelInfo modelInfo, String modelPath) {
        List<String> env = new ArrayList<>();
        List<String> volumes = new ArrayList<>();
        env.add(fineTuneTrainEnvTaskName + "=" + modelInfo.getId());
        env.add(fineTuneTrainEnvModelPathName + "=" + fineTuneTrainModelHostDir);
        volumes.add(fineTuneTrainDataVolume + ":/home/data/");
        volumes.add(fineTuneTrainModelHostDir + modelPath + ":/home/model/");
        return DockerProp.builder()
                .imageName("pollux_finetune")
                .imageTag("v001")
                .containerName("pollux-finetune-" + modelInfo.getId())
                .env(env)
                .volumes(volumes)
                .build();
    }
}
