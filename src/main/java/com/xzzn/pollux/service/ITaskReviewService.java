package com.xzzn.pollux.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xzzn.pollux.entity.TaskReview;
import com.xzzn.pollux.model.vo.response.user.UserInfo;

import java.util.List;

/**
 * <p>
 * 任务审核表 服务类
 * </p>
 *
 * @author xzzn
 */
public interface ITaskReviewService extends IService<TaskReview> {

    List<UserInfo> getReviewUserList(String taskId, String keyword);

    void addReviewer(String userId, String taskId, String operatorUserId);

    void deleteReviewer(String userId, String taskId, String operatorUserId);
}
