package com.xzzn.pollux.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserReviewProgressDTO {
    private long total;
    private long totalReviewed;
    private long curAllocated;
    private long curReviewed;
    private double totalProgress;
    private double curProgress;
}
