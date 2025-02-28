package com.xzzn.pollux.model.vo.request.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

@Data
@Getter
@Setter
public class ModelAddBaseRequest implements Serializable {
    @NotBlank(message = "模型名称不能为空")
    String modelName;

    String modelIntro;
    String modelParam;
    String modelType;
    String modelScene;
}
