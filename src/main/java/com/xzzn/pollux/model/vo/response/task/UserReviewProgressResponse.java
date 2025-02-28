package com.xzzn.pollux.model.vo.response.task;

import com.xzzn.pollux.model.vo.response.user.UserInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserReviewProgressResponse implements Serializable {
    private UserInfo user;
    private long total;
    private long totalReviewed;
    private long curAllocated;
    private long curReviewed;
    private double totalProgress;
    private double curProgress;
}