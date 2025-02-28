package com.xzzn.pollux.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xzzn.pollux.common.exception.BusinessException;
import com.xzzn.pollux.entity.QATask;
import com.xzzn.pollux.entity.ReviewConfig;
import com.xzzn.pollux.mapper.ReviewConfigMapper;
import com.xzzn.pollux.model.pojo.ScoreButtonInfo;
import com.xzzn.pollux.model.vo.response.task.ReviewConfigQueryResponse;
import com.xzzn.pollux.service.IReviewConfigService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 * 任务审核配置表 服务实现类
 * </p>
 *
 * @author xzzn
 */
@Service
public class ReviewConfigServiceImpl extends ServiceImpl<ReviewConfigMapper, ReviewConfig> implements IReviewConfigService {

    @Resource
    private QATaskServiceImpl qaTaskService;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public boolean hasTwoStep(String taskId) {
        ReviewConfig config = this.lambdaQuery().eq(ReviewConfig::getTaskId, taskId).one();
        return config.getIsStepTwo();
    }

    @Override
    public void setReviewConfig(String userId, String taskId, String qReviewCriteria, String aReviewCriteria, Boolean isStepTwo,
                                String scoreReviewCriteria, List<ScoreButtonInfo> scoreButtonInfoList) {
        QATask qaTask = checkReviewConfigExistence(taskId, true);
        if (!qaTask.getTaskCreatorId().equals(userId)) {
            throw new BusinessException(500, "该用户没有权限设置审核配置");
        }
        checkButton(scoreButtonInfoList);
        String buttonJSON = transButtonToJSON(scoreButtonInfoList);

        ReviewConfig reviewConfigInfo = ReviewConfig.builder()
                .taskId(taskId)
                .qReviewCriteria(qReviewCriteria)
                .aReviewCriteria(aReviewCriteria)
                .isStepTwo(isStepTwo)
                .scoreReviewCriteria(scoreReviewCriteria)
                .scoreButtonInfo(buttonJSON)
                .build();

        save(reviewConfigInfo);

        qaTask.setReviewConfigId(reviewConfigInfo.getId());
        qaTaskService.updateById(qaTask);

    }

    @Override
    public ReviewConfigQueryResponse getReviewConfig(String taskId) {
        QATask qaTask = checkReviewConfigExistence(taskId, false);

        ReviewConfig reviewConfig = getById(qaTask.getReviewConfigId());
        List<ScoreButtonInfo> buttonInfoList = transJSONToButton(reviewConfig);

        return ReviewConfigQueryResponse.builder()
                .taskId(reviewConfig.getTaskId())
                .qReviewCriteria(reviewConfig.getQReviewCriteria())
                .aReviewCriteria(reviewConfig.getAReviewCriteria())
                .isStepTwo(reviewConfig.getIsStepTwo())
                .scoreReviewCriteria(reviewConfig.getScoreReviewCriteria())
                .scoreButtonInfo(buttonInfoList)
                .build();
    }

    @Override
    public Boolean isSetReviewConfig(String taskId) {
        QATask qaTask = qaTaskService.getById(taskId);
        if (qaTask == null) {
            throw new BusinessException(500, "任务不存在");
        }
        return qaTask.getReviewConfigId() != null;
    }

    private QATask checkReviewConfigExistence(String taskId, boolean isExist) {
        QATask qaTask = qaTaskService.getById(taskId);
        if ((qaTask.getReviewConfigId() != null) == isExist) {
            throw new BusinessException(500, isExist ? "任务已设置审核配置" : "任务未设置审核配置");
        }
        return qaTask;
    }


    private void checkButton(List<ScoreButtonInfo> scoreButtonInfoList) {
        // 判断button的数量是否合规
        if (scoreButtonInfoList.size() < 2 || scoreButtonInfoList.size() > 5) {
            throw new BusinessException(500, "打分按钮数量不得少于2个,且不得多余5个");
        }
    }

    private String transButtonToJSON(List<ScoreButtonInfo> scoreButtonInfoList) {
        try {
            return new ObjectMapper().writeValueAsString(scoreButtonInfoList);
        } catch (JsonProcessingException e) {
            throw new BusinessException(500, "Button JSON序列化失败");
        }
    }

    private List<ScoreButtonInfo> transJSONToButton(ReviewConfig reviewConfig) {
        try {
            return objectMapper.readValue(reviewConfig.getScoreButtonInfo(), new TypeReference<List<ScoreButtonInfo>>() {
            });
        } catch (JsonProcessingException e) {
            throw new BusinessException(500, "Button JSON反序列化失败");
        }
    }
}
