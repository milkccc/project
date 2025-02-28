package com.xzzn.pollux.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xzzn.pollux.common.enums.QATaskFilesEnum;
import com.xzzn.pollux.common.enums.QATaskM2BStatusEnum;
import com.xzzn.pollux.common.enums.TaskStatusEnum;
import com.xzzn.pollux.entity.*;
import com.xzzn.pollux.mapper.QATaskMapper;
import com.xzzn.pollux.model.vo.response.task.QATaskM2BResponse;
import com.xzzn.pollux.service.ESService;
import com.xzzn.pollux.service.S3Service;
import com.xzzn.pollux.service.impl.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;


import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;

@Slf4j
@Component
public class QATaskMqListener {

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private ESService esService;

    @Resource
    private QATaskFilesServiceImpl qaTaskFilesService;

    @Resource
    private QATaskServiceImpl qaTaskService;

    @Resource
    private QATaskMapper qaTaskMapper;

    @Resource
    private FileInfoServiceImpl fileInfoService;

    @Resource
    private DatasetInfoServiceImpl datasetInfoService;

    @Resource
    private QATaskDatasetsServiceImpl qaTaskDatasetsService;

    @Resource
    private S3Service s3Service;

    @RabbitListener(queues = "qa.task.model2backend.queue")
    public void listenQATaskM2BQueue(Message message) throws IOException {
        //log.debug("队列qa.task.m2b.queue接收消息成功");
        log.debug("队列qa.task.model2backend.queue接收消息成功");

        byte[] body = message.getBody();
        QATaskM2BResponse response = objectMapper.readValue(body, QATaskM2BResponse.class);
        //log.debug("队列qa.task.m2b.queue收到的响应请求");
        log.debug("队列qa.task.model2backend.queue收到的响应请求");
        //log.info("返回的response是：{}",response);

        if (response == null) {
            //log.error("队列qa.task.m2b.queue收到响应为null");
            log.error("队列qa.task.model2backend.queue收到响应为null");
            return;
        }

        handleQATaskM2BResponse(response);
    }


    private void handleQATaskM2BResponse(QATaskM2BResponse response) {
        String taskId = response.getTaskId();
        String fileId = response.getFileId();
        String status = response.getStatus().toString();
        List<QATaskM2BResponse.Chunk> chunkList = response.getChunkList();
        try {
            // 任务失败的状态维护（只要有文件失败就改变任务状态）
            if (status.equals(QATaskM2BStatusEnum.FAILED.toString())) {
                handleFailStatus(taskId, fileId);
            } else {
                handleSuccessStatus(taskId, fileId, chunkList);
            }
            qaTaskMapper.incrementComplete(taskId);
            QATask qaTask = qaTaskMapper.selectById(taskId);
            if (qaTask.getComplete().equals(qaTask.getTotal())) {
                handleFinishStatus(qaTask);
            }
        }catch (NullPointerException e){
            log.error("未监听到任务 handleQATaskM2BResponse: {}", e.getMessage());
        }
    }

    private void handleFinishStatus(QATask qaTask) {
        Long count = qaTaskFilesService.lambdaQuery()
                .eq(QATaskFiles::getTaskId, qaTask.getId())
                .eq(QATaskFiles::getStatus, QATaskFilesEnum.SUCCESS)
                .count();
        String status;
        if (count == 0) {
            status = TaskStatusEnum.FAILED.toString();
        } else if (count < qaTask.getTotal()) {
            status = TaskStatusEnum.ERROR.toString();
        } else {
            status = TaskStatusEnum.SUCCESS.toString();
        }
        qaTask.setTaskStatus(status);
        qaTaskMapper.updateById(qaTask);
    }

    private void handleFailStatus(String taskId, String fileId) {
        try {
            qaTaskFilesService.lambdaUpdate()
                    .eq(QATaskFiles::getTaskId, taskId)
                    .eq(QATaskFiles::getFileId, fileId)
                    .set(QATaskFiles::getStatus, QATaskFilesEnum.FAILED);
            qaTaskService.lambdaUpdate().eq(QATask::getId, taskId)
                    .set(QATask::getTaskStatus, TaskStatusEnum.ERROR.toString()).update();
        }
        catch (NullPointerException e){
            log.error("未监听到任务 handleFailStatus: {}", e.getMessage());
        }
    }


    private void handleSuccessStatus(String taskId, String fileId, List<QATaskM2BResponse.Chunk> chunkList) {
        try {
            String datasetId = fileInfoService.lambdaQuery().eq(FileInfo::getId, fileId).one().getDatasetId();
            int qaCount = esService.saveQAToES(taskId, datasetId, fileId, chunkList);
            updateQATaskFilesQaCountAndStatus(taskId, fileId, qaCount);
            qaTaskMapper.updateQACount(taskId, qaCount);

            qaTaskFilesService.lambdaUpdate()
                    .eq(QATaskFiles::getTaskId, taskId)
                    .eq(QATaskFiles::getId, fileId)
                    .set(QATaskFiles::getStatus, QATaskFilesEnum.SUCCESS);
        }catch (NullPointerException e){
            log.error("未监听到任务 handleSuccessStatus: {}", e.getMessage());
        }
    }

    private void updateQATaskFilesQaCountAndStatus(String taskId, String fileId, int qaCount) {
        try {
            qaTaskFilesService.lambdaUpdate()
                    .eq(QATaskFiles::getTaskId, taskId)
                    .eq(QATaskFiles::getFileId, fileId)
                    .set(QATaskFiles::getQaCount, qaCount)
                    .set(QATaskFiles::getStatus, QATaskFilesEnum.SUCCESS)
                    .update();
        }
        catch (NullPointerException e){
            log.error("未监听到任务 updateQATaskFilesQaCountAndStatus: {}", e.getMessage());
        }
    }
    public void deletesampleQA(String taskId, String fileId) {//在指定任务里删除QA对逻辑
        QATask qaTask = qaTaskMapper.selectById(taskId);
        esService.deleteByFileId(taskId, fileId);
        qaTaskMapper.deleteTotalandCompleteCount(taskId);
        int count = qaTaskFilesService.lambdaQuery()
                .eq(QATaskFiles::getTaskId, taskId)
                .eq(QATaskFiles::getFileId, fileId)
                .select(QATaskFiles::getQaCount)
                .list()
                .stream()
                .mapToInt(QATaskFiles::getQaCount)  // 提取每个记录的 qaCount 值
                .sum();
        int currentQaCount = qaTask.getQaCount();

        String datasetId = fileInfoService.lambdaQuery()
                .eq(FileInfo::getId, fileId).
                one()
                .getDatasetId();
        qaTaskDatasetsService.lambdaUpdate()
                .eq(QATaskDatasets::getTaskId, taskId)
                .eq(QATaskDatasets::getDatasetId, datasetId)
                .remove();
        qaTaskFilesService.lambdaUpdate()
                .eq(QATaskFiles::getTaskId, taskId)
                .eq(QATaskFiles::getFileId, fileId)
                .remove();
        fileInfoService.lambdaUpdate()
                .eq(FileInfo::getId, fileId)
                .eq(FileInfo::getDatasetId, datasetId)
                .remove();
        datasetInfoService.lambdaUpdate()
                .eq(DatasetInfo::getId, datasetId)
                .remove();
        s3Service.deleteDirectory(datasetId);
        qaTaskService.lambdaUpdate()
                .eq(QATask::getId, taskId)
                .set(QATask::getQaCount,currentQaCount-count)
                .update();
        esService.deleteByFileId(taskId,fileId);
    }
}