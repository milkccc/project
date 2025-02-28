package com.xzzn.pollux.model.vo.request.qa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

/**
 * QA修改请求
 *
 * @author xzzn
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QAUpdateRequest {

    @NotBlank
    private String taskId;

    @NotBlank
    private String id;

    @NotBlank
    private String question;

    @NotBlank
    private String answer;
}
