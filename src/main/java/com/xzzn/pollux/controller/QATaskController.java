package com.xzzn.pollux.controller;

import com.xzzn.pollux.common.BaseResponse;
import com.xzzn.pollux.common.DeleteRequest;
import com.xzzn.pollux.common.ListResponse;
import com.xzzn.pollux.common.ResultResponse;
import com.xzzn.pollux.entity.QATask;
import com.xzzn.pollux.entity.User;
import com.xzzn.pollux.listener.AutogenTaskMqListener;
import com.xzzn.pollux.listener.QATaskMqListener;
import com.xzzn.pollux.model.pojo.ScoreButtonInfo;
import com.xzzn.pollux.model.pojo.TaskConfigMap;
import com.xzzn.pollux.model.vo.request.qa.QAImportRequest;
import com.xzzn.pollux.model.vo.request.task.*;
import com.xzzn.pollux.model.vo.response.task.QATaskListResponse;
import com.xzzn.pollux.model.vo.response.task.QATaskProblemTreeResponse;
import com.xzzn.pollux.model.vo.response.task.QaTaskTreeResponse;
import com.xzzn.pollux.model.vo.response.task.ReviewConfigQueryResponse;
import com.xzzn.pollux.service.ESService;
import com.xzzn.pollux.service.S3Service;
import com.xzzn.pollux.service.impl.*;
import com.xzzn.pollux.utils.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/task")
@Slf4j
public class QATaskController {
    @Resource
    private QATaskServiceImpl qaTaskService;

    @Resource
    private UserServiceImpl userService;

    @Resource
    private TaskReviewServiceImpl taskReviewService;

    @Resource
    private ReviewConfigServiceImpl reviewConfigService;

    @Resource
    private DatasetInfoServiceImpl datasetInfoService;

    @Resource
    private ESService esService;

    @Resource
    private QATaskAutoCreateRequest qaTaskAutoCreateRequest;



    @PostMapping
    public ResultResponse<String> createQATask(
            @RequestHeader("ACCESS-KEY") String accessKey,
            @RequestBody QATaskCreateRequest qaTaskCreateRequest
    ) {
        User user = userService.getUser(accessKey);
        String userId = user.getId();

        String taskName = qaTaskCreateRequest.getTaskName();
        List<String> datasetList = qaTaskCreateRequest.getDatasetList();
        TaskConfigMap taskConfigMap = qaTaskCreateRequest.getTaskConfigMap();
        String domain = qaTaskCreateRequest.getDomain();
        String description = qaTaskCreateRequest.getDescription();
        boolean priority = qaTaskCreateRequest.isPriority();
        String taskId = qaTaskService.createQATask(userId, taskName, datasetList, taskConfigMap, domain, description, priority);

        return ResultUtils.success(taskId);
    }


    @PostMapping("/import")
    public BaseResponse importQAs(
            @RequestHeader("ACCESS-KEY") String accessKey,
            @Valid @ModelAttribute QAImportRequest qaImportRequest
    )  {
        List<MultipartFile> importQAs = qaImportRequest.getImportQAs();
        int fileCount = importQAs.size();

        List<MultipartFile> processedFiles = importQAs;

        String taskName = qaImportRequest.getTaskName();
        String userId = userService.getUser(accessKey).getId();
        QATask qaTask = qaTaskService.buildAndSavelocalQATask(userId, taskName);
        //esService.createQAIndex(qaTask.getId());
        taskReviewService.addReviewer(userId, qaTask.getId(), userId);

        return qaTaskService.batchhandleTaskbeforeImport(processedFiles, taskName, userId, qaTask, fileCount);
    }


    @GetMapping
    public ResultResponse<QATask> getQATask(
            @RequestHeader("ACCESS-KEY") String accessKey,
            @RequestParam String taskId) {
        return ResultUtils.success(qaTaskService.getQATask(taskId));
    }

    @GetMapping("/list")
    public ListResponse<ArrayList<QATaskListResponse.QATaskInfoVO>> getQATasks(
            @RequestHeader("ACCESS-KEY") String accessKey,
            @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "10") Integer size,
            @RequestParam(value = "sortAttribute", required = false, defaultValue = "createTime") String sortAttribute,
            @RequestParam(value = "sortDirection", required = false, defaultValue = "desc") String sortDirection,
            @RequestParam(value = "taskName", required = false) String taskName,
            @RequestParam(value = "taskId", required = false) String taskId,
            @RequestParam(value = "taskStatus", required = false) String taskStatus,
            @RequestParam(value = "taskCreator", required = false) String taskCreator,
            @RequestParam(value = "createTime", required = false) Long createTime,
            @RequestParam(value = "endTime", required = false) Long endTime
    ) {
        User user = userService.getUser(accessKey);
        QATaskListResponse qaTaskListResponses = qaTaskService.getQATasks(page, size, sortAttribute,
                sortDirection, taskName, taskId, taskStatus, taskCreator, createTime, endTime, user.getId());
        return new ListResponse<>(200, qaTaskListResponses.getQaTaskInfoVOList(), qaTaskListResponses.getTotal(), "success");
    }

    @DeleteMapping
    public BaseResponse deleteQATasks(
            @RequestHeader("ACCESS-KEY") String accessKey,
            @RequestBody DeleteRequest deleteRequest
    ) {
        List<String> deleteIds = deleteRequest.getDeleteIds();
        qaTaskService.deleteQATasks(deleteIds);
        qaTaskService.deleteQATasksinEs(deleteIds);
        return BaseResponse.success();
    }

    @PutMapping
    public BaseResponse renameQATask(
            @RequestHeader("ACCESS-KEY") String accessKey,
            @RequestParam("taskId") String taskId,
            @RequestParam("taskName") String taskName
    ) {
        qaTaskService.renameQATask(taskId, taskName);
        return BaseResponse.success();
    }

    @GetMapping("/tree")
    public ResultResponse<ArrayList<QaTaskTreeResponse>> getTreeForTask(
            @RequestHeader("ACCESS-KEY") String accessKey,
            @RequestParam("taskId") String taskId
    ) {
        return ResultUtils.success((ArrayList<QaTaskTreeResponse>) qaTaskService.getTreeForTask(taskId));
    }

    @PostMapping("/review-config")
    public BaseResponse setReviewConfig(
            @RequestHeader("ACCESS-KEY") String accessKey,
            @RequestBody ReviewConfigRequest reviewConfigRequest
    ) {
        User user = userService.getUser(accessKey);

        String taskId = reviewConfigRequest.getTaskId();
        String qReviewCriteria = reviewConfigRequest.getQReviewCriteria();
        String aReviewCriteria = reviewConfigRequest.getAReviewCriteria();
        Boolean isStepTwo = reviewConfigRequest.getIsStepTwo();
        String scoreReviewCriteria = reviewConfigRequest.getScoreReviewCriteria();
        List<ScoreButtonInfo> scoreButtonInfoList = reviewConfigRequest.getScoreButtonInfoList();

        reviewConfigService.setReviewConfig(user.getId(), taskId, qReviewCriteria, aReviewCriteria, isStepTwo,
                scoreReviewCriteria, scoreButtonInfoList);
        return BaseResponse.success();
    }

    @GetMapping("/review-config")
    public ResultResponse<ReviewConfigQueryResponse> getReviewConfig(
            @RequestHeader("ACCESS-KEY") String accessKey,
            @RequestParam("taskId") String taskId
    ) {
        return ResultUtils.success(reviewConfigService.getReviewConfig(taskId));
    }

    @GetMapping("/review-config/is-set")
    public ResultResponse<Boolean> isSetReviewConfig(
            @RequestHeader("ACCESS-KEY") String accessKey,
            @RequestParam("taskId") String taskId
    ) {
        return ResultUtils.success(reviewConfigService.isSetReviewConfig(taskId));
    }

    @GetMapping("/problem")
    public ResultResponse<QATaskProblemTreeResponse> getQATaskProblem(
            @RequestHeader("ACCESS-KEY") String accessKey,
            @RequestParam("taskId") String taskId
    ) {
        return ResultUtils.success(qaTaskService.getQATaskProblem(taskId));
    }

    @DeleteMapping("/problem")
    public BaseResponse deleteQATaskFiles(
            @RequestHeader("ACCESS-KEY") String accessKey,
            @RequestBody QATaskDeleteProblemRequest qaTaskDeleteProblemRequest
    ) {
        User user = userService.getUser(accessKey);
        String taskId = qaTaskDeleteProblemRequest.getTaskId();
        List<String> fileIdList = qaTaskDeleteProblemRequest.getFileIdList();

        qaTaskService.deleteQATaskFiles(user.getId(), taskId, fileIdList);
        return BaseResponse.success();
    }

    @PostMapping("/problem")
    public BaseResponse retryQATask(
            @RequestHeader("ACCESS-KEY") String accessKey,
            @Valid @RequestBody QATaskRetryRequest qaTaskRetryRequest
    ) {
        User user = userService.getUser(accessKey);
        String taskId = qaTaskRetryRequest.getTaskId();
        List<String> fileIdList = qaTaskRetryRequest.getFileIdList();

        qaTaskService.retryQATask(user.getId(), taskId, fileIdList);
        return BaseResponse.success();
    }

    @Autowired
    private S3Service s3Service;
    @Autowired
    private AutogenTaskMqListener autogenTaskMqListener;


    @GetMapping("/download")
    public CompletableFuture<ResultResponse<String>> downloadFile(@RequestHeader("ACCESS-KEY") String accessKey,
                                                                  @RequestParam String url, HttpServletResponse response) {
        try {
            // 从 URL 提取对象名称
            String objectName = url.split("static-pollux/")[1];

            List<File> tempFile = null;
            int retryCount = 100;
            int retryInterval = 1000; // 1 秒
            for (int i = 0; i < retryCount; i++) {
                //tempFile = s3Service.downloadObjectToFile(objectName);
                tempFile = s3Service.downloadAllFilesInDirectory(objectName);

                if (tempFile != null) {
                    break;
                }
                Thread.sleep(retryInterval);
            }


            log.info("文件已下载");
            String userId = userService.getUser(accessKey).getId();
            String taskName = qaTaskAutoCreateRequest.getTaskName();
            TaskConfigMap taskConfigMap = qaTaskAutoCreateRequest.getTaskConfigMap();
            String domain = qaTaskAutoCreateRequest.getDomain();
            String description = qaTaskAutoCreateRequest.getDescription();
            boolean priority = qaTaskAutoCreateRequest.isPriority();

            return autogenTaskMqListener.generatetask(tempFile, userId, taskName, taskConfigMap, domain, description, priority,objectName);
        } catch (IOException e) {
            return CompletableFuture.completedFuture(ResultUtils.error(500, "任务创建错误"));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    @Autowired
    private WebScraperImpl webScraper;
    @Autowired
    private WebScraperPlusImpl webScraperPlus;

    @GetMapping("/crawler")
    public void downloadFile(@RequestHeader("ACCESS-KEY") String accessKey,
                                                                  @RequestParam String filename) {
        webScraperPlus.webScraper(filename);
    }

    @Autowired
    private QATaskMqListener qaTaskMqListener;

    @GetMapping("/deletesampleQA")
    public void deletesampleQA(
            @RequestHeader("ACCESS-KEY") String accessKey,
            @RequestParam("taskId") String taskId,
            @RequestParam("fileId") String fileId){
        qaTaskMqListener.deletesampleQA(taskId, fileId);
    }

}

