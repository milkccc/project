package com.xzzn.pollux.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xzzn.pollux.entity.ReviewConfig;
import com.xzzn.pollux.model.pojo.ScoreButtonInfo;
import com.xzzn.pollux.model.vo.response.task.ReviewConfigQueryResponse;

import java.util.List;

/**
 * <p>
 * 任务审核配置表 服务类
 * </p>
 *
 * @author xzzn
 */
public interface IReviewConfigService extends IService<ReviewConfig> {
    boolean hasTwoStep(String taskId);

    void setReviewConfig(String userId, String taskId, String qReviewCriteria, String aReviewCriteria, Boolean isStepTwo,
                         String scoreReviewCriteria, List<ScoreButtonInfo> scoreButtonInfoList);

    ReviewConfigQueryResponse getReviewConfig(String taskId);

    Boolean isSetReviewConfig(String taskId);
}
