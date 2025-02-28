package com.xzzn.pollux.model.vo.request.qa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QAPageQueryRequest {

    @NotBlank(message = "任务ID不能为空")
    @NotNull
    private String taskId;

    @PositiveOrZero(message = "页码必须为正数或零")
    private Integer page;

    @Positive(message = "每页大小必须为正数")
    private Integer pageSize;

    private List<String> fileIdList;

    private String keyword;

    private Boolean isReview;

    private String allocateUserId;
}
