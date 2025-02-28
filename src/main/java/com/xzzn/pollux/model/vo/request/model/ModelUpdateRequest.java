package com.xzzn.pollux.model.vo.request.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

@Data
@Getter
@Setter
public class ModelUpdateRequest implements Serializable {
    @NotBlank(message = "模型ID不能为空")
    String modelId;

    @NotBlank(message = "模型名称不能为空")
    String modelName;

    String modelIntro;

    String modelScene;
}
