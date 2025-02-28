package com.xzzn.pollux.model.vo.request.qa;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;


/**
 * QA导入请求
 *
 * @author xzzn
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@NotNull
public class QAImportRequest {

    @NotBlank(message = "任务名称,不能为空")
    private String taskName;

    @ApiModelProperty(value = "QAs")
    @NotNull
    private List<MultipartFile> importQAs;

}
