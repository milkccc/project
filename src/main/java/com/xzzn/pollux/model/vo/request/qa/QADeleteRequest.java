package com.xzzn.pollux.model.vo.request.qa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import java.util.List;

/**
 * QA删除请求
 *
 * @author xzzn
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QADeleteRequest {

    @NotBlank(message = "任务ID,不能为空")
    private String taskId;

    private List<QADeleteInfo> qaDeleteInfoList;

    private List<String> fileIdList;

    private List<ExcludeInfo> excludeInfoList;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QADeleteInfo {
        @NotBlank
        private String fileId;

        @NotBlank
        private List<String> ids;
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
