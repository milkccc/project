package com.xzzn.pollux.model.vo.request.model;

import lombok.Data;

import javax.validation.constraints.*;
import java.io.Serializable;
import java.util.List;

@Data
public class ModelTrainRequest implements Serializable {

    @NotBlank(message = "基座模型ID不能为空")
    private String baseModelId;

    @NotBlank(message = "模型名称不能为空")
    private String modelName;

    private String modelIntro;

    @NotEmpty(message = "训练任务ID列表不能为空")
    private List<String> taskIdList;

    @NotNull(message = "测试集比例不能为空")
    @DecimalMin(value = "0.0", message = "测试集比例不能小于0")
    @DecimalMax(value = "1.0", message = "测试集比例不能大于1")
    private Double testSetRatio;

    @NotBlank(message = "训练策略不能为空")
    private String trainStrategy;

    @NotNull(message = "迭代轮次不能为空")
    @Min(value = 1, message = "迭代轮次不能小于1")
    private Integer iterationRound;

    @NotNull
    @DecimalMin(value = "0.000001", message = "学习率不能小于0.000001")
    private Double learningRate;

    @NotNull
    @Min(value = 1, message = "批大小不能小于1")
    @Max(value = 128, message = "批大小不能大于128")
    private Integer batchSize;

}
