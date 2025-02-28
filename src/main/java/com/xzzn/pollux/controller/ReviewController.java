package com.xzzn.pollux.controller;

import com.xzzn.pollux.common.BaseResponse;
import com.xzzn.pollux.common.ResultResponse;
import com.xzzn.pollux.entity.User;
import com.xzzn.pollux.entity.es.QADocument;
import com.xzzn.pollux.model.dto.UserReviewProgressDTO;
import com.xzzn.pollux.model.vo.request.review.ReviewNormalRequest;
import com.xzzn.pollux.model.vo.request.review.ReviewScoreRequest;
import com.xzzn.pollux.model.vo.request.review.TaskReviewRequest;
import com.xzzn.pollux.model.vo.response.task.UserReviewProgressResponse;
import com.xzzn.pollux.model.vo.response.user.UserInfo;
import com.xzzn.pollux.service.ESService;
import com.xzzn.pollux.service.ReviewService;
import com.xzzn.pollux.service.impl.TaskReviewServiceImpl;
import com.xzzn.pollux.service.impl.UserServiceImpl;
import com.xzzn.pollux.utils.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * 审核控制类
 *
 * @author xzzn
 */
@Slf4j
@RestController
@RequestMapping("/review")
public class ReviewController {

    @Resource
    private ReviewService reviewService;

    @Resource
    private TaskReviewServiceImpl taskReviewService;

    @Resource
    private UserServiceImpl userService;

    @Resource
    private ESService esService;

    @PostMapping("/normal")
    public BaseResponse normalReview(
            @RequestHeader("ACCESS-KEY") String accessKey,
            @Valid @RequestBody ReviewNormalRequest reviewNormalRequest
    ) {
        User user = userService.getUser(accessKey);
        String taskId = reviewNormalRequest.getTaskId();
        String id = reviewNormalRequest.getId();

        reviewService.normalReview(user.getId(), taskId, id);
        return BaseResponse.success();
    }

    @PostMapping("/score")
    public BaseResponse scoreReview(
            @RequestHeader("ACCESS-KEY") String accessKey,
            @Valid @RequestBody ReviewScoreRequest reviewScoreRequest
    ) {
        User user = userService.getUser(accessKey);
        String taskId = reviewScoreRequest.getTaskId();
        String id = reviewScoreRequest.getId();
        String score = reviewScoreRequest.getScore();

        reviewService.scoreReview(user.getId(), taskId, id, score);
        return BaseResponse.success();
    }

    @PostMapping("/reviewer")
    public BaseResponse addReviewer(
            @RequestHeader("ACCESS-KEY") String accessKey,
            @Valid @RequestBody TaskReviewRequest taskReviewRequest
    ) {
        String operateUserId = userService.getUser(accessKey).getId();
        String userId = taskReviewRequest.getUserId();
        String taskId = taskReviewRequest.getTaskId();
        taskReviewService.addReviewer(userId, taskId, operateUserId);
        return BaseResponse.success();
    }

    @DeleteMapping("/reviewer")
    public BaseResponse deleteReviewer(
            @RequestHeader("ACCESS-KEY") String accessKey,
            @Valid @RequestBody TaskReviewRequest taskReviewRequest
    ) {
        User user = userService.getUser(accessKey);
        String taskId = taskReviewRequest.getTaskId();
        String userId = taskReviewRequest.getUserId();
        taskReviewService.deleteReviewer(userId, taskId, user.getId());
        reviewService.deallocateQA(taskId, userId, user.getId());
        return BaseResponse.success();
    }


    @GetMapping("/reviewer/list")
    public ResultResponse<ArrayList<UserInfo>> getReviewers(
            @RequestHeader("ACCESS-KEY") String accessKey,
            @RequestParam String taskId,
            @RequestParam(value = "UserName", required = false) String UserName
    ) {
        return ResultUtils.success((ArrayList<UserInfo>) taskReviewService.getReviewUserList(taskId,UserName));
    }


    @GetMapping("/progress")
    public ResultResponse<UserReviewProgressResponse> getReviewProgress(
            @RequestHeader("ACCESS-KEY") String accessKey,
            @RequestParam String taskId,
            @RequestParam String userId
    ) throws InvocationTargetException, IllegalAccessException {
        User user = userService.getUser(accessKey);

        UserReviewProgressDTO reviewProgress = reviewService.getReviewProgress(taskId, userId, user.getId());

        UserReviewProgressResponse userReviewProgressResponse = new UserReviewProgressResponse();
        BeanUtils.copyProperties(userReviewProgressResponse, reviewProgress);
        userReviewProgressResponse.setUser(userService.getUserInfoById(userId));

        return ResultUtils.success(userReviewProgressResponse);
    }

    @GetMapping("/progress/all")
    public ResultResponse<ArrayList<UserReviewProgressResponse>> getAllReviewProgress(
            @RequestHeader("ACCESS-KEY") String accessKey,
            @RequestParam String taskId,
            @RequestParam(value = "UserName", required = false) String UserName
    ) throws InvocationTargetException, IllegalAccessException {
        User user = userService.getUser(accessKey);
        List<UserInfo> userInfoList = taskReviewService.getReviewUserList(taskId,UserName);
        ArrayList<UserReviewProgressResponse> userReviewProgressResponseList = new ArrayList<>();

        for (UserInfo userInfo : userInfoList) {
            UserReviewProgressResponse userReviewProgressResponse = new UserReviewProgressResponse();
            UserReviewProgressDTO reviewProgress = reviewService.getReviewProgress(taskId, userInfo.getId(), user.getId());
            BeanUtils.copyProperties(userReviewProgressResponse, reviewProgress);
            userReviewProgressResponse.setUser(userInfo);
            userReviewProgressResponseList.add(userReviewProgressResponse);
        }
        return ResultUtils.success(userReviewProgressResponseList);
    }

    @GetMapping("/allocated/qas")
    public ResultResponse<ArrayList<QADocument>> getAllocatedQAs(
            @RequestHeader("ACCESS-KEY") String accessKey,
            @RequestParam("taskId") String taskId,
            @RequestParam("userId") String userId
    ) {
        User user = userService.getUser(accessKey);
        List<String> allocatedQAIds = reviewService.getAllocatedQAIds(taskId, userId, user.getId());
        List<QADocument> qaDocumentByIds = esService.getQADocumentByIds(taskId, allocatedQAIds);
        return ResultUtils.success((ArrayList<QADocument>) qaDocumentByIds);
    }
}
