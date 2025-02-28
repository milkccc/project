package com.xzzn.pollux.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xzzn.pollux.entity.ModelInfo;
import com.xzzn.pollux.model.vo.response.model.ModelListResponse;

import java.util.List;

/**
 * <p>
 * 模型信息表 服务类
 * </p>
 *
 * @author xzzn
 */
public interface IModelInfoService extends IService<ModelInfo> {

    ModelInfo getModel(String userId, String modelId);

    ModelListResponse getModelInfoList(Integer page, Integer size, String sortAttribute, String sortDirection,
                                       String modelName, String modelStatus, String modelCategory, String userId);

    void deleteModels(List<String> modelIds);

    String addBaseModel(String modelName, String modelIntro, String modelType, String modelParam, String modelScene);

    void updateModel(String modelId, String modelName, String modelIntro, String modelScene);

    void startModelFineTuning(String baseModelId, String modelName, String modelIntro, List<String> datasetIdList,
                              Double datasetTestProp, String trainStrategy, Integer iterationRound, Double learningRate,
                              Integer batchSize, String userId);

    String getTrainProgress(String modelId, String id);
}
