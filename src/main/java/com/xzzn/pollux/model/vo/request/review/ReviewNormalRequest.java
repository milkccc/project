package com.xzzn.pollux.model.vo.request.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

/**
 * 普通审核请求体
 *
 * @author xzzn
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewNormalRequest {

    @NotBlank
    private String taskId;

    @NotBlank
    private String id;
}
