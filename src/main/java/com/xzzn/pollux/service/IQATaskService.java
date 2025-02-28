package com.xzzn.pollux.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xzzn.pollux.common.ResultResponse;
import com.xzzn.pollux.entity.QATask;
import com.xzzn.pollux.model.pojo.TaskConfigMap;
import com.xzzn.pollux.model.vo.response.task.QATaskListResponse;
import com.xzzn.pollux.model.vo.response.task.QATaskProblemTreeResponse;
import com.xzzn.pollux.model.vo.response.task.QaTaskTreeResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * <p>
 * 推理任务表 服务类
 * </p>
 *
 * @author xzzn
 */
public interface IQATaskService extends IService<QATask> {

    String createQATask(String userId, String taskName, List<String> datasetList, TaskConfigMap taskConfigMap, String domain, String description,boolean priority);

    QATaskListResponse getQATasks(Integer page, Integer size, String sortAttribute, String sortDirection,
                                  String taskName, String taskId, String taskStatus, String taskCreator, Long createTime,
                                  Long endTime, String userId);

    void deleteQATasks(List<String> deleteIds);

    QATask getQATask(String taskId);

    void renameQATask(String taskId, String taskName);

    List<QaTaskTreeResponse> getTreeForTask(String taskId);

    QATaskProblemTreeResponse getQATaskProblem(String taskId);

    void deleteQATaskFiles(String id, String taskId, List<String> fileIdList);

    void retryQATask(String id, String taskId, List<String> fileIdList);


}

