package com.xzzn.pollux.controller;


import com.xzzn.pollux.common.BaseResponse;
import com.xzzn.pollux.common.ListResponse;
import com.xzzn.pollux.common.ResultResponse;
import com.xzzn.pollux.entity.ModelInfo;
import com.xzzn.pollux.entity.User;
import com.xzzn.pollux.model.vo.request.model.ModelAddBaseRequest;
import com.xzzn.pollux.model.vo.request.model.ModelTrainRequest;
import com.xzzn.pollux.model.vo.request.model.ModelUpdateRequest;
import com.xzzn.pollux.model.vo.response.model.ModelListResponse;
import com.xzzn.pollux.service.impl.ModelInfoServiceImpl;
import com.xzzn.pollux.service.impl.UserServiceImpl;
import com.xzzn.pollux.utils.ResultUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 模型信息表 前端控制器
 * </p>
 *
 * @author xzzn
 */
@RestController
@RequestMapping("/model")
public class ModelController {

    @Resource
    private UserServiceImpl userService;

    @Resource
    private ModelInfoServiceImpl modelInfoService;

    @GetMapping
    public ResultResponse<ModelInfo> getModel(
            @RequestHeader("ACCESS-KEY") String accessKey,
            @RequestParam("modelId") String modelId
    ) {
        User user = userService.getUser(accessKey);
        return ResultUtils.success(modelInfoService.getModel(user.getId(), modelId));
    }

    @GetMapping("/list")
    public ListResponse<ArrayList<ModelListResponse.ModelInfoVO>> getModels(
            @RequestHeader("ACCESS-KEY") String accessKey,
            @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "100") Integer size,
            @RequestParam(value = "sortAttribute", required = false, defaultValue = "createTime") String sortAttribute,
            @RequestParam(value = "sortDirection", required = false, defaultValue = "desc") String sortDirection,
            @RequestParam(value = "modelName", required = false) String modelName,
            @RequestParam(value = "modelStatus", required = false) String modelStatus,
            @RequestParam(value = "modelCategory", required = false) String modelCategory
    ) {
        User user = userService.getUser(accessKey);
        ModelListResponse modelInfoList = modelInfoService.getModelInfoList(page, size, sortAttribute, sortDirection, modelName,
                modelStatus, modelCategory, user.getId());
        return new ListResponse<>(200, modelInfoList.getModelInfoVOList(), modelInfoList.getTotal(), "success");
    }

    @DeleteMapping
    public BaseResponse deleteModels(
            @RequestHeader("ACCESS-KEY") String accessKey,
            @RequestParam("modelIds") List<String> modelIds) {
        modelInfoService.deleteModels(modelIds);
        return BaseResponse.success();
    }

    @PutMapping
    public BaseResponse updateModel(
            @RequestHeader("ACCESS-KEY") String accessKey,
            @Valid @RequestBody ModelUpdateRequest modelUpdateRequest
    ) {
        String modelId = modelUpdateRequest.getModelId();
        String modelName = modelUpdateRequest.getModelName();
        String modelIntro = modelUpdateRequest.getModelIntro();
        String modelScene = modelUpdateRequest.getModelScene();

        modelInfoService.updateModel(modelId, modelName, modelIntro, modelScene);

        return BaseResponse.success();
    }

    @PostMapping("/base")
    public ResultResponse<String> addBaseModel(
            @RequestHeader("ACCESS-KEY") String accessKey,
            @Valid @RequestBody ModelAddBaseRequest modelAddBaseRequest
    ) {
        String modelName = modelAddBaseRequest.getModelName();
        String modelIntro = modelAddBaseRequest.getModelIntro();
        String modelParam = modelAddBaseRequest.getModelParam();
        String modelType = modelAddBaseRequest.getModelType();
        String modelScene = modelAddBaseRequest.getModelScene();

        return ResultUtils.success(modelInfoService.addBaseModel(modelName, modelIntro, modelType, modelParam, modelScene));
    }

    @PostMapping("/train")
    public BaseResponse trainModel(
            @RequestHeader("ACCESS-KEY") String accessKey,
            @Valid @RequestBody ModelTrainRequest modelTrainRequest
    ) {
        String baseModelId = modelTrainRequest.getBaseModelId();
        String modelName = modelTrainRequest.getModelName();
        String modelIntro = modelTrainRequest.getModelIntro();
        List<String> taskIdList = modelTrainRequest.getTaskIdList();
        Double testSetRatio = modelTrainRequest.getTestSetRatio();
        String trainStrategy = modelTrainRequest.getTrainStrategy();
        Integer iterationRound = modelTrainRequest.getIterationRound();
        Double learningRate = modelTrainRequest.getLearningRate();
        Integer batchSize = modelTrainRequest.getBatchSize();

        User user = userService.getUser(accessKey);

        modelInfoService.startModelFineTuning(baseModelId, modelName, modelIntro, taskIdList, testSetRatio,
                trainStrategy, iterationRound, learningRate, batchSize, user.getId());

        return BaseResponse.success();
    }

    @GetMapping("/train")
    public ResultResponse<String> getTrainProgress(
            @RequestHeader("ACCESS-KEY") String accessKey,
            @RequestParam("modelId") String modelId
    ) {
        User user = userService.getUser(accessKey);

        return ResultUtils.success(modelInfoService.getTrainProgress(modelId, user.getId()));
    }
}
