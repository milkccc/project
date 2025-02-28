package com.xzzn.pollux.model.vo.request.qa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * QA分配请求
 *
 * @author xzzn
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@NotNull
public class QAAllocateRequest {

    @NotBlank(message = "任务ID,不能为空")
    private String taskId;

    private String UserId;

    private List<QAAllocate> qaAllocate;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QAAllocate {
        private String fileId;
        private Integer allocateCount;
    }

}
