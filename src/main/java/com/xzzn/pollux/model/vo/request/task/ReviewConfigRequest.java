package com.xzzn.pollux.model.vo.request.task;

import com.xzzn.pollux.model.pojo.ScoreButtonInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReviewConfigRequest {

    private String taskId;

    /**
     * 问题审核标准
     */
    private String qReviewCriteria;

    /**
     * 答案审核标准
     */
    private String aReviewCriteria;

    /**
     * 是否进行第二步审核
     */
    private Boolean isStepTwo;

    /**
     * 打分审核标准
     */
    private String scoreReviewCriteria;

    /**
     * 打分按钮信息
     */
    private List<ScoreButtonInfo> scoreButtonInfoList;
}
