package com.xzzn.pollux.model.vo.request.dataset;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 文件导入请求
 *
 * @author xzzn
 */
@Data
@ApiModel(description = "数据集导入请求")
public class DatasetImportRequest {

    @ApiModelProperty(value = "数据集")
    @NotNull
    private MultipartFile importDataset;

    @ApiModelProperty(value = "标签")
    private List<String> tags;
}
