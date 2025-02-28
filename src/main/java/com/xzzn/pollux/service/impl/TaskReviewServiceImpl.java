package com.xzzn.pollux.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xzzn.pollux.common.exception.BusinessException;
import com.xzzn.pollux.entity.QATask;
import com.xzzn.pollux.entity.TaskReview;
import com.xzzn.pollux.mapper.QATaskMapper;
import com.xzzn.pollux.mapper.TaskReviewMapper;
import com.xzzn.pollux.model.vo.response.user.UserInfo;
import com.xzzn.pollux.service.ITaskReviewService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 任务审核表 服务实现类
 * </p>
 *
 * @author xzzn
 */
@Service
public class TaskReviewServiceImpl extends ServiceImpl<TaskReviewMapper, TaskReview> implements ITaskReviewService {

    @Resource
    private UserServiceImpl userService;

    @Resource
    private QATaskMapper qaTaskMapper;

    @Override
    public List<UserInfo> getReviewUserList(String taskId, String keyword) {
        List<String> userIdList = this.lambdaQuery().eq(TaskReview::getTaskId, taskId).list()
                .stream()
                .map(TaskReview::getUserId)
                .collect(Collectors.toList());

        List<UserInfo> userInfoList = userService.getUserInfoListByIdList(userIdList);

        if (keyword == null || keyword.isEmpty()) {
            return userInfoList;
        }

        return userInfoList.stream()
                .filter(userInfo -> userInfo.getUserName().contains(keyword))
                .collect(Collectors.toList());
    }

    @Override
    public void addReviewer(String userId, String taskId, String operateUserId) {
        hasCreatorPermission(taskId, operateUserId);
        TaskReview taskReview = TaskReview.builder()
                .userId(userId)
                .taskId(taskId).build();
        checkIfExistReviewer(userId, taskId);
        this.save(taskReview);
    }

    @Override
    public void deleteReviewer(String userId, String taskId, String operateUserId) {
        hasCreatorPermission(taskId, operateUserId);
        if (userId.equals(operateUserId)) {
            throw new BusinessException(500, "可见范围无法删除创建者");
        }
        QueryWrapper<TaskReview> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        queryWrapper.eq("task_id", taskId);
        this.remove(queryWrapper);
    }

    public void hasCreatorPermission(String taskId, String operateUserId) {
        QATask qaTask = qaTaskMapper.selectById(taskId);
        if (!operateUserId.equals(qaTask.getTaskCreatorId())) {
            throw new BusinessException(500, "该用户没有权限执行此操作");
        }
    }

    private void checkIfExistReviewer(String userId, String taskId) {
        TaskReview existingReview = this.lambdaQuery().eq(TaskReview::getUserId, userId)
                .eq(TaskReview::getTaskId, taskId)
                .one();
        if (existingReview != null) {
            throw new BusinessException(500, "已存在该审核人");
        }
    }

    public void deallocateTaskReview(String taskId, String userId) {
        TaskReview taskReview = this.lambdaQuery().eq(TaskReview::getTaskId, taskId)
                .eq(TaskReview::getUserId, userId).one();
        taskReview.setReviewCurNum(0);
        taskReview.setAllocatedQAList("[]");
        taskReview.setAllocatedQANum(0);
        this.updateById(taskReview);
    }

}
