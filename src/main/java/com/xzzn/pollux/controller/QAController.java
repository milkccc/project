package com.xzzn.pollux.controller;

import com.xzzn.pollux.common.BaseResponse;
import com.xzzn.pollux.common.ResultResponse;
import com.xzzn.pollux.entity.User;
import com.xzzn.pollux.model.vo.request.qa.QAAllocateRequest;
import com.xzzn.pollux.model.vo.request.qa.QADeleteRequest;
import com.xzzn.pollux.model.vo.request.qa.QAExportRequest;
import com.xzzn.pollux.model.vo.request.qa.QAUpdateRequest;
import com.xzzn.pollux.model.vo.response.task.FileContentQueryResponse;
import com.xzzn.pollux.model.vo.response.task.QAPairPageResponse;
import com.xzzn.pollux.model.vo.response.task.QAUnallocatedFileIdResponse;
import com.xzzn.pollux.service.ESService;
import com.xzzn.pollux.service.QAService;
import com.xzzn.pollux.service.ReviewService;
import com.xzzn.pollux.service.impl.UserServiceImpl;
import com.xzzn.pollux.utils.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.swing.tree.TreeNode;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * QA控制类
 *
 * @author xzzn
 */
@Slf4j
@RestController
@RequestMapping("/qa")
public class QAController {

    @Resource
    private QAService qaService;

    @Resource
    private ESService esService;

    @Resource
    private ReviewService reviewService;

    @Resource
    private UserServiceImpl userService;

    @DeleteMapping
    public BaseResponse deleteQAs(
            @RequestHeader("ACCESS-KEY") String accessKey,
            @Valid @RequestBody QADeleteRequest qaDeleteRequest
    ) {
        qaService.deleteQA(qaDeleteRequest);
        return BaseResponse.success();
    }

    @PutMapping
    public BaseResponse updateQA(
            @RequestHeader("ACCESS-KEY") String accessKey,
            @Valid @RequestBody QAUpdateRequest qaUpdateRequest
    ) {
        qaService.updateQA(qaUpdateRequest);
        return BaseResponse.success();
    }

    @PostMapping("/export")
    public ResponseEntity<String> exportQAs(
            @RequestHeader("ACCESS-KEY") String accessKey,
            @Valid @RequestBody QAExportRequest qaExportRequest
    ) {
        // 获取JSON格式的QA对
        String jsonContent = qaService.exportQA(qaExportRequest);
        log.info("获取的QA的JSON字符串为:{}", jsonContent);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=data.json");
        headers.add("Content-Type", "application/json;charset=UTF-8");

        return ResponseEntity
                .ok()
                .headers(headers)
                .body(jsonContent);
    }

    @PostMapping("/list")
    public ResultResponse<QAPairPageResponse> getQAs(
            @RequestHeader("ACCESS-KEY") String accessKey,
            @RequestParam("taskId") String taskId,
            @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize,
            @RequestParam(value = "fileIdList", required = false) List<String> fileIdList,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "isReview", required = false) Boolean isReview,
            @RequestParam(value = "allocateUserId", required = false) String allocateUserId,
            @RequestParam(value = "score", required = false) String score
    ) {
        if (!esService.existQAIndex(taskId)) {
            return ResultUtils.error(500, "该任务尚未创建QA对或任务不存在");
        }
        return ResultUtils.success(esService.getQADocumentPage(taskId, page, pageSize, fileIdList, keyword, isReview,
                allocateUserId, score));
    }

    @GetMapping("/allocate")
    public BaseResponse allocateQAs(
            @RequestHeader("ACCESS-KEY") String accessKey,
            @RequestParam("taskId") String taskId,
            @RequestParam("userId") String userId
    ) {
        reviewService.allocateQA(taskId, userId);
        return BaseResponse.success();
    }

    @GetMapping("/equalAllocateQA")
    public BaseResponse equalAllocate(
            @RequestHeader("ACCESS-KEY") String accessKey,
            @RequestParam("taskId") String taskId,
            @RequestParam("userIds") List<String> userIds
    ) throws InterruptedException {

        reviewService.equalAllocate(taskId, userIds);
        return BaseResponse.success();
    }

    @GetMapping("/allocateQACount")
    public ResultResponse<QAUnallocatedFileIdResponse> countUnallocatedQAByFileIds(
            @RequestHeader("ACCESS-KEY") String accessKey,
            @RequestParam("taskId") String taskId
    ) {

        return ResultUtils.success(reviewService.countUnallocatedQAByFileIds(taskId));
    }

    @GetMapping("/allocateByFile")
    public BaseResponse allocateQAsToUsers(
            @RequestHeader("ACCESS-KEY") String accessKey,
            @Valid @RequestBody QAAllocateRequest qaAllocateRequest
    ) {
        String taskId = qaAllocateRequest.getTaskId();
        String userId = qaAllocateRequest.getUserId();
        List<QAAllocateRequest.QAAllocate> qaAllocations = qaAllocateRequest.getQaAllocate();

        for (QAAllocateRequest.QAAllocate qaAllocate : qaAllocations) {
            String fileId = qaAllocate.getFileId();
            int count = qaAllocate.getAllocateCount();
            reviewService.allocateQAsToUsers(taskId, userId, fileId, count);
        }
        return BaseResponse.success();
    }

    @GetMapping("/deallocate")
    public BaseResponse deallocateQAs(
            @RequestHeader("ACCESS-KEY") String accessKey,
            @RequestParam("taskId") String taskId,
            @RequestParam("userId") String userId
    ) {
        User user = userService.getUser(accessKey);
        reviewService.deallocateQA(taskId, userId, user.getId());
        return BaseResponse.success();
    }

    @GetMapping("/file-content")
    public ResultResponse<FileContentQueryResponse> getFileContent(
            @RequestHeader("ACCESS-KEY") String accessKey,
            @RequestParam("taskId") String taskId,
            @RequestParam("qaId") String qaId
    ) {
        return ResultUtils.success(esService.getFileContentsByQAId(taskId, qaId));
    }

}
