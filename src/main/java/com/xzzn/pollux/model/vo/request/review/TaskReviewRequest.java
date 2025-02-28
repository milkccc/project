package com.xzzn.pollux.model.vo.request.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TaskReviewRequest {

    private String userId;

    private String taskId;
}
