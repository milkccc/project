package com.xzzn.pollux.model.vo.request.qa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * QA导出请求
 *
 * @author xzzn
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@NotNull
public class QAExportRequest {

    @NotNull(message = "是否导出全部,不能为空")
    private boolean all;

    @NotBlank(message = "任务ID,不能为空")
    private String taskId;

    private List<QAExportInfo> qaExportInfoList;

    private List<String> fileIdList;

    private List<ExcludeInfo> excludeInfoList;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QAExportInfo {
        private String fileId;

        private List<String> qaIds;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExcludeInfo {
        @NotBlank
        private String fileId;

        @NotBlank
        private List<String> ids;
    }
}
