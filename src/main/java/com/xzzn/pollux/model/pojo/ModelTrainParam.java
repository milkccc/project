package com.xzzn.pollux.model.pojo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ModelTrainParam {

    private String baseModel;

    private Double testSetRatio;

    private String trainStrategy;

    private Integer iterationRound;

    private Double learningRate;

    private Integer batchSize;
}
