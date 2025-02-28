package com.xzzn.pollux.model.vo.request.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

/**
 * 打分审核请求体
 *
 * @author xzzn
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewScoreRequest {

    @NotBlank
    private String taskId;

    @NotBlank
    private String id;

    @NotBlank
    private String score;
}
