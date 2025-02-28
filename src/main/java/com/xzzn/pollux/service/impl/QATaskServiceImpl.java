package com.xzzn.pollux.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xzzn.pollux.common.BaseResponse;
import com.xzzn.pollux.common.ResultResponse;
import com.xzzn.pollux.common.enums.DatasetStatusEnum;
import com.xzzn.pollux.common.enums.FileTypeEnum;
import com.xzzn.pollux.common.enums.QATaskFilesEnum;
import com.xzzn.pollux.common.enums.TaskStatusEnum;
import com.xzzn.pollux.common.exception.BusinessException;
import com.xzzn.pollux.mapper.DatasetInfoMapper;
import com.xzzn.pollux.service.S3Service;
import com.xzzn.pollux.utils.CustomMultipartFile;
import com.xzzn.pollux.entity.*;
import com.xzzn.pollux.mapper.FileInfoMapper;
import com.xzzn.pollux.mapper.QATaskLocalMapper;
import com.xzzn.pollux.mapper.QATaskMapper;
import com.xzzn.pollux.model.pojo.FileTreeNode;
import com.xzzn.pollux.model.pojo.FileTreeNodeForProblem;
import com.xzzn.pollux.model.pojo.FileTreeNodeForTask;
import com.xzzn.pollux.model.pojo.TaskConfigMap;
import com.xzzn.pollux.model.vo.request.task.QATaskB2MRequest;
import com.xzzn.pollux.model.vo.response.task.QATaskListResponse;
import com.xzzn.pollux.model.vo.response.task.QATaskProblemTreeResponse;
import com.xzzn.pollux.model.vo.response.task.QaTaskTreeResponse;
import com.xzzn.pollux.service.ESService;
import com.xzzn.pollux.service.IQATaskService;


import com.xzzn.pollux.utils.ByteArrayMultipartFile;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.xzzn.pollux.utils.ResultUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.*;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * <p>
 * 推理任务表 服务实现类
 * </p>
 *
 * @author xzzn
 */
@Service
@Slf4j
public class QATaskServiceImpl extends ServiceImpl<QATaskMapper, QATask> implements IQATaskService {

    @Resource
    private AsyncTaskExecutor generateQATaskExecutor;

    @Resource
    private ESService esService;

    @Resource
    private S3Service s3Service;


    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private DatasetInfoServiceImpl datasetInfoService;

    @Resource
    private FileInfoServiceImpl fileInfoService;

    @Resource
    private QATaskDatasetsServiceImpl qaTaskDatasetsService;

    @Resource
    private QATaskFilesServiceImpl qaTaskFilesService;

    @Resource
    private UserServiceImpl userService;

    @Autowired
    private QATaskLocalMapper qaTaskLocalMapper;

    @Autowired
    private FileInfoMapper fileInfoMapper;

    @Resource
    private DatasetInfoMapper datasetInfoMapper;


    @Resource
    private TaskReviewServiceImpl taskReviewService;

    @Value("${qa.task.exchange}")
    private String qaTaskExchange;

//    @Value("${qa.task.b2m.queue.rk}")
//    private String qaTaskB2MQueueRoutingKey;

    @Value("${qa.task.backend2model.queue.rk}")
    private String qaTaskbackend2modelQueueRoutingKey;
    @Qualifier("fileUploadTaskExecutor")

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createQATask(String userId, String taskName, List<String> datasetList,
                               TaskConfigMap taskConfigMap, String domain, String description,boolean priority) {
        log.debug("开始创建任务 {} ", taskName);

        checkDatasetStatus(datasetList);

        List<FileInfo> fileInfoList = fileInfoService.lambdaQuery()
                .in(FileInfo::getDatasetId, datasetList).list();

        QATask qaTask = buildAndSaveQATask(userId, taskName, taskConfigMap, domain, description, fileInfoList);

        buildAndSaveQATaskDatasets(qaTask.getId(), taskName, datasetList);

        buildAndSaveQATaskFiles(qaTask.getId(), taskName, fileInfoList.stream().map(FileInfo::getId).collect(Collectors.toList()));

        taskReviewService.addReviewer(userId, qaTask.getId(), userId);

        submitAsyncGenerateTask(qaTask, fileInfoList, priority, userId);

        esService.createQAIndex(qaTask.getId());

        log.info("新建QA生成任务 {} ,任务id为 {}", taskName, qaTask.getId());

        return qaTask.getId();
    }

    @Override
    public QATaskListResponse getQATasks(Integer page, Integer size, String sortAttribute, String sortDirection,
                                         String taskName, String taskId, String taskStatus, String taskCreator, Long createTime,
                                         Long endTime, String userId) {
        QueryWrapper<QATask> queryWrapper = new QueryWrapper<>();
        List<String> viewableQATaskIdList = getViewableQATaskIdList(userId);

        if (!viewableQATaskIdList.isEmpty()) {
            queryWrapper.lambda().in(QATask::getId, viewableQATaskIdList);
        } else {
            return buildQATasksResponses(new ArrayList<>(), 0L);
        }
        if (StringUtils.isNotBlank(taskName)) {
            queryWrapper.lambda().like(QATask::getTaskName, taskName);
        }
        if (StringUtils.isNotBlank(taskId)) {
            queryWrapper.lambda().eq(QATask::getId, taskId);
        }
        if (StringUtils.isNotBlank(taskStatus)) {
            queryWrapper.lambda().eq(QATask::getTaskStatus, taskStatus);
        }
        if (StringUtils.isNotBlank(taskCreator)) {
            queryWrapper.lambda().eq(QATask::getTaskCreatorId, taskCreator);
        }
        if (createTime != null) {
            queryWrapper.lambda().eq(QATask::getCreateTime, createTime);
        }
        if (endTime != null) {
            queryWrapper.lambda().eq(QATask::getTaskEndTime, endTime);
        }
        // 根据排序属性和排序方向来进行排序
        if (StringUtils.isNotBlank(sortAttribute) && StringUtils.isNotBlank(sortDirection)) {
            if ("asc".equalsIgnoreCase(sortDirection)) {
                queryWrapper.orderByAsc(StringUtils.camelToUnderline(sortAttribute));
            } else if ("desc".equalsIgnoreCase(sortDirection)) {
                queryWrapper.orderByDesc(StringUtils.camelToUnderline(sortAttribute));
            }
        }

        Long totalCount = this.count(queryWrapper);
        List<QATask> qaTaskList = this.page(new Page<>(page, size), queryWrapper).getRecords();

        return buildQATasksResponses(qaTaskList, totalCount);
    }

    @Override
    @Transactional
    public void deleteQATasks(List<String> deleteIds) {
        baseMapper.deleteBatchIds(deleteIds);
    }

    public void deleteQATasksinEs(List<String> deleteIds) {
        for (String deleteId : deleteIds) {
            try {
                esService.deleteQAIndex(deleteId);
            } catch (Exception e) {
                log.warn("Failed to delete QA index in Elasticsearch for ID: " + deleteId, e);
            }
        }
    }

    @Override
    public QATask getQATask(String taskId) {
        QATask qaTask = this.getById(taskId);
        if (qaTask == null) {
            throw new BusinessException(500, "任务不存在");
        }
        return qaTask;
    }

    @Override
    public void renameQATask(String taskId, String taskName) {
        this.lambdaUpdate().eq(QATask::getId, taskId).set(QATask::getTaskName, taskName).update();
    }

    @Override
    public List<QaTaskTreeResponse> getTreeForTask(String taskId) {
        List<QaTaskTreeResponse> response = new ArrayList<>();
        List<String> datasetIdList = getDatasetIdListByTaskId(taskId);

        for (String datasetId : datasetIdList) {
            DatasetInfo datasetInfo = datasetInfoService.getById(datasetId);

            String tree = datasetInfo.getTree();

            if (StringUtils.isBlank(tree)) {
                continue;
            }
            FileTreeNode curNode = parseFileTreeNode(tree);

            FileTreeNodeForTask newNode = buildTreeForTask(curNode, taskId);
            response.add(new QaTaskTreeResponse(datasetId, newNode));
        }

        return response;
    }

    @Override
    public QATaskProblemTreeResponse getQATaskProblem(String taskId) {
        QATask qaTask = getQATask(taskId);

        List<String> fileIdList = getFailedFileIdListByTaskId(qaTask.getId());

        List<QATaskProblemTreeResponse.QATaskProblem> problems = getTreeForProblem(taskId, fileIdList);
        return new QATaskProblemTreeResponse(taskId, problems);
    }


    @Override
    @Transactional
    public void deleteQATaskFiles(String id, String taskId, List<String> fileIdList) {
        // 减去QA数量(大概率为0)
        int qaCount = qaTaskFilesService.lambdaQuery()
                .eq(QATaskFiles::getTaskId, taskId)
                .in(QATaskFiles::getFileId, fileIdList)
                .list()
                .stream()
                .mapToInt(QATaskFiles::getQaCount)
                .sum();
        this.baseMapper.updateQACount(taskId, -qaCount);
        fileIdList.forEach(fileId -> esService.deleteByFileId(taskId, fileId));
        // 减去完成数量和总数量
        updateQATaskFiles(taskId, fileIdList, true);
        QATask qaTask = getQATask(taskId);
        // 重新判断任务状态
        updateQATaskStatus(qaTask);
    }

    @Override
    @Transactional
    public void retryQATask(String id, String taskId, List<String> fileIdList) {
        // 减去完成数量
        updateQATaskFiles(taskId, fileIdList, false);
        QATask qaTask = getQATask(taskId);
        // 修改文件状态为PROCESSING
        qaTaskFilesService.lambdaUpdate()
                .eq(QATaskFiles::getTaskId, taskId)
                .in(QATaskFiles::getFileId, fileIdList)
                .set(QATaskFiles::getStatus, QATaskFilesEnum.PROCESSING)
                .update();
        // 重新判断任务状态
        updateQATaskStatus(qaTask);
        // 重新发送消息
        for (String fileId : fileIdList) {
            FileInfo fileInfo = fileInfoService.getById(fileId);
            sendMessage(qaTask, fileInfo);
        }
    }

    private void updateQATaskFiles(String taskId, List<String> fileIdList, boolean isDelete) {
        int size = qaTaskFilesService.lambdaQuery()
                .eq(QATaskFiles::getTaskId, taskId)
                .in(QATaskFiles::getFileId, fileIdList)
                .count().intValue();
        if (isDelete) {
            this.baseMapper.updateTotalCount(taskId, -size);
            qaTaskFilesService.lambdaUpdate()
                    .eq(QATaskFiles::getTaskId, taskId)
                    .in(QATaskFiles::getFileId, fileIdList)
                    .remove();
        } else {
            this.baseMapper.updateCompleteCount(taskId, -size);
        }
    }

    private void updateQATaskStatus(QATask qaTask) {
        int total = qaTask.getTotal();
        int complete = qaTask.getComplete();
        long successCount = qaTaskFilesService.lambdaQuery()
                .eq(QATaskFiles::getTaskId, qaTask.getId())
                .eq(QATaskFiles::getStatus, QATaskFilesEnum.SUCCESS)
                .count();
        TaskStatusEnum status = judgeQATaskStatus(complete, total, (int) successCount);
        this.lambdaUpdate().eq(QATask::getId, qaTask.getId()).set(QATask::getTaskStatus, status.toString()).update();
    }

    private TaskStatusEnum judgeQATaskStatus(int complete, int total, int successCount) {
        if (complete == total) {
            if (successCount == 0) {
                return TaskStatusEnum.FAILED;
            } else if (successCount == complete) {
                return TaskStatusEnum.SUCCESS;
            }
        } else if (successCount == complete) {
            return TaskStatusEnum.PROCESSING;
        }
        return TaskStatusEnum.ERROR;
    }

    private List<QATaskDatasets> buildQATaskDatasetsList(String taskId, List<String> datasetList) {
        return datasetList.stream()
                .map(datasetId -> {
                    QATaskDatasets qaTaskDatasets = new QATaskDatasets();
                    qaTaskDatasets.setTaskId(taskId);
                    qaTaskDatasets.setDatasetId(datasetId);
                    qaTaskDatasets.setStatus(TaskStatusEnum.PROCESSING.toString());
                    return qaTaskDatasets;
                })
                .collect(Collectors.toList());
    }

    private List<QATaskFiles> buildQATaskFilesList(String taskId, List<String> fileIdList) {
        return fileIdList.stream()
                .map(fileId -> {
                    QATaskFiles qaTaskFiles = new QATaskFiles();
                    qaTaskFiles.setTaskId(taskId);
                    qaTaskFiles.setFileId(fileId);
                    qaTaskFiles.setStatus(QATaskFilesEnum.PROCESSING.toString());
                    return qaTaskFiles;
                })
                .collect(Collectors.toList());
    }

    private void submitAsyncGenerateTask(QATask qaTask, List<FileInfo> fileInfoList,boolean priority,String userId) {
        generateQATaskExecutor.submit(() -> {
            try {
                asyncGenerateTask(qaTask, fileInfoList,priority,userId);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }


    private void asyncGenerateTask(QATask qaTask, List<FileInfo> fileInfoList, boolean priority,String userId) throws IOException, InterruptedException {
        FileInfo minFileInfo = minfile(fileInfoList);
        // 如果优先级为 true，并且 fileInfoList 不为空，挑选最小的文件发送
        if (priority && !fileInfoList.isEmpty()) {
            if(minFileInfo.getFileSize() < 8 ) {
                sendMessagehandsample(qaTask, minFileInfo);
                // 处理其余文件
                for (FileInfo fileInfo : fileInfoList) {
                    if (!fileInfo.equals(minFileInfo)) {
                        sendMessage(qaTask, fileInfo);
                    }
                }
            }
            else{
                List<FileInfo> minFile = downloadSplitandupload(minFileInfo,userId,qaTask);
                sendMessagehandsample(qaTask, minFile.get(0));
                //sendMessage(qaTask, minFile.get(1));
                log.info("成功发送小样文本！");
                // 处理其余文件
                for (FileInfo fileInfo : fileInfoList) {
                    sendMessage(qaTask, fileInfo);
                }
            }
        } else {
            for (FileInfo fileInfo : fileInfoList) {
                sendMessage(qaTask, fileInfo);
            }
        }
    }


    private List<FileInfo> downloadSplitandupload(FileInfo minFileInfo,String userId,QATask qaTask) throws IOException, InterruptedException {
        //从网址中下载，按2000字划分，生成两个文件
        String url = minFileInfo.getParseFilePath();
        // 从 URL 提取对象名称
        String objectName = url.split("static-pollux/")[1];

        File tempFile = null;
        int retryCount = 20;
        int retryInterval = 1000; // 1 秒
        for (int i = 0; i < retryCount; i++) {
            tempFile = s3Service.downloadObjectToFile(objectName,"小样文件-");
                if (tempFile != null && tempFile.exists() && tempFile.length() > 0) {
                    break;
                }
            Thread.sleep(retryInterval);
        }

        List<File> splitfile = splitFileByCharacters(tempFile,2000);

        List<String> filelist = uploaddata(splitfile,userId,qaTask);

        FileInfo samplefilelittle  = fileInfoService.getById(filelist.get(0));

        List<FileInfo> fileInfoList = new ArrayList<>();
        fileInfoList.add(samplefilelittle);

        return fileInfoList;
    }

    public List<String> uploaddata(List<File> files, String userId,QATask qaTask) throws IOException, InterruptedException {
        List<String> fileList = new ArrayList<>();
        List<String> datasetList = new ArrayList<>();
        Optional<File> smallestFile = files.stream()
                .min((file1, file2) -> Long.compare(file1.length(), file2.length()));
        File file = null;
        if (smallestFile.isPresent()) {
            file = smallestFile.get();
        }
        //for (File file : files) {
            try {
                MultipartFile multipartFile = new CustomMultipartFile(file);

                CompletableFuture<String> datasetIdFuture = datasetInfoService.generateDataSetlocalQA(userId, multipartFile, null);
                String datasetId = null;
                String fileId = null;
                int retryCount = 20;
                int retryInterval = 1000; // 1 秒

                for (int i = 0; i < retryCount; i++) {
                    datasetId = datasetIdFuture.get();
                    if (datasetId != null && !datasetId.isEmpty()) {
                        break;
                    }
                    Thread.sleep(retryInterval);
                }
                for (int i = 0; i < retryCount; i++) {
                    fileId = fileInfoMapper.selectIdByDatasetId(datasetId);
                    if (fileId != null && !fileId.isEmpty()) {
                        break;
                    }
                    Thread.sleep(retryInterval);
                }

                // 将生成的 fileId 添加到列表中
                if (fileId != null && !fileId.isEmpty()) {
                    fileList.add(fileId);
                    log.info("数据集上传完成，数据集ID：{}", fileId);
                } else {
                    log.error("Failed to generate fileId for file: {}" , file.getName());
                }
                // 将生成的 datasetId 添加到列表中
                if (datasetId != null && !datasetId.isEmpty()) {
                    datasetList.add(datasetId);
                    log.info("数据集上传完成，数据集ID：{}", datasetId);
                } else {
                    log.error("Failed to generate datasetId for file:{} " , file.getName());
                }

                buildAndSaveQATaskDatasets(qaTask.getId(), qaTask.getTaskName(), datasetList);
                buildAndSaveQATaskFiles(qaTask.getId(), qaTask.getTaskName(), fileList);
                this.baseMapper.updateonlyTotalCount(qaTask.getId(), 1);
                //this.baseMapper.updateCompleteCount(qaTask.getId(), -1);

            } catch (IOException | ExecutionException | InterruptedException e) {
                log.error("Error processing file:{} " , file.getName());
                e.printStackTrace();
            }
        return fileList;
    }


    public static List<File> splitFileByCharacters(File sourceFile, int maxCharsPerFile) throws IOException {
        List<File> resultFiles = new ArrayList<>();
        String extension = "";
        String fileName = sourceFile.getName();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            extension = fileName.substring(dotIndex);
        }
        log.info("读取源文件后缀名: {}",extension);
        // 读取源文件的内容
        String content = new String(Files.readAllBytes(sourceFile.toPath()));

        int totalLength = content.length();

        // 如果总字符数小于等于maxCharsPerFile，则不划分
        if (totalLength <= maxCharsPerFile) {
            resultFiles.add(sourceFile);
            return resultFiles;
        }
        // 分割第一个文件（2000字符）
        String firstPart = content.substring(0, maxCharsPerFile);
        File firstFile = new File(sourceFile.getParent(), sourceFile.getName() + "_part1.txt");
        try (FileWriter writer = new FileWriter(firstFile)) {
            writer.write(firstPart);
        }
        resultFiles.add(firstFile);
        // 分割剩余的部分
        String secondPart = content.substring(maxCharsPerFile);
        File secondFile = new File(sourceFile.getParent(), sourceFile.getName() + "_part2.txt");
        try (FileWriter writer = new FileWriter(secondFile)) {
            writer.write(secondPart);
        }
        //resultFiles.add(secondFile);

        return resultFiles;
    }


    private void sendMessage(QATask qaTask, FileInfo fileInfo) {
        QATaskB2MRequest request = QATaskB2MRequest.builder()
                .taskId(qaTask.getId())
                .fileId(fileInfo.getId())
                .path(fileInfo.getParseFilePath())
                .splitLevel(qaTask.getSplitLevel())
                .densityLevel(qaTask.getDensityLevel())
                .description(qaTask.getDescription())
                .domain(qaTask.getDomain())
                .priority(1)
                .build();
        try {
            // 将 QATaskB2MRequest 对象转换为字节数组
            byte[] messageBody = new ObjectMapper().writeValueAsBytes(request);

            // 使用 MessageBuilder 构建消息并设置优先级为 1
            Message message = MessageBuilder.withBody(messageBody)
                    .setPriority(1)
                    .build();

            // 发送消息
            rabbitTemplate.send(qaTaskExchange, qaTaskbackend2modelQueueRoutingKey, message);
            log.debug("普通级：任务 {} 向交换机 {} 绑定的key {} 发送了消息 {}", qaTask.getId(), qaTaskExchange, qaTaskbackend2modelQueueRoutingKey, request);
        } catch (Exception e) {
            throw new BusinessException(500, "任务向模型侧发送消息失败");
        }
    }

    private void sendMessagehandsample(QATask qaTask, FileInfo fileInfo) {
        QATaskB2MRequest request = QATaskB2MRequest.builder()
                .taskId(qaTask.getId())
                .fileId(fileInfo.getId())
                .path(fileInfo.getParseFilePath())
                .splitLevel(qaTask.getSplitLevel())
                .densityLevel(qaTask.getDensityLevel())
                .description(qaTask.getDescription())
                .domain(qaTask.getDomain())
                .priority(10)
                .build();
        try {
            // 将 QATaskB2MRequest 对象转换为字节数组
            byte[] messageBody = new ObjectMapper().writeValueAsBytes(request);

            // 使用 MessageBuilder 构建消息并设置优先级为 10
            Message message = MessageBuilder.withBody(messageBody)
                    .setPriority(10)
                    .build();

            // 发送消息
            rabbitTemplate.send(qaTaskExchange, qaTaskbackend2modelQueueRoutingKey, message);
            log.debug("优先级：任务 {} 向交换机 {} 绑定的key {} 发送了消息 {}", qaTask.getId(), qaTaskExchange, qaTaskbackend2modelQueueRoutingKey, request);
        } catch (Exception e) {
            throw new BusinessException(500, "任务向模型侧发送消息失败");
        }
    }

    public void checkDatasetStatus(List<String> datasetList) {
        if(datasetInfoService.lambdaQuery().eq(DatasetInfo::getDatasetStatus, DatasetStatusEnum.SUCCESS)
                .in(DatasetInfo::getId, datasetList).count() != datasetList.size()) {
            throw new BusinessException(500, "推理任务需要已解析完成的数据集");
        }
    }
    public BaseResponse batchhandleTaskbeforeImport(List<MultipartFile> importQAs, String taskName, String userId,QATask qaTask,int fileCount){
        for (MultipartFile file : importQAs) {
            handleTaskbeforeImport(file,taskName,userId,qaTask,fileCount);
        }
        return BaseResponse.success();
    }

    //@Transactional
    public BaseResponse handleTaskbeforeImport(MultipartFile importQAs, String taskName, String userId,QATask qaTask,int fileCount){
        CompletableFuture<String> datasetIdFuture = datasetInfoService.generateDataSetlocalQA(userId, importQAs, null);
        try {
            // 轮询获取 fileId，最多重试 10 次，每次间隔 1 秒
            String fileId = null;
            String datasetId = null;
            int retryCount = 10;
            int retryInterval = 1000; // 1 秒

            for (int i = 0; i < retryCount; i++) {
                datasetId = datasetIdFuture.get();
                if (datasetId != null && !datasetId.isEmpty()) {
                    break;
                }
                Thread.sleep(retryInterval);
            }
            if (datasetId == null || datasetId.isEmpty()) {
                log.error("获取 datasetId 失败");
                return BaseResponse.error("获取 datasetId 失败");
            }

            for (int i = 0; i < retryCount; i++) {
                fileId = fileInfoMapper.selectIdByDatasetId(datasetId);
                if (fileId != null && !fileId.isEmpty()) {
                    break;
                }
                Thread.sleep(retryInterval);
            }

            if (fileId == null || fileId.isEmpty()) {
                log.error("文件id为空：文件解析失败");
                return BaseResponse.error("文件id为空：文件解析失败");
            }
            else if ("csv".equals(fileInfoMapper.selectFileTypeById(datasetId))){
                datasetInfoMapper.CompleteSetone(datasetId);
                datasetInfoMapper.updateStatus(datasetId);
            }

            List<String> fileIdList = convertStringToList(fileId);
            List<String> datasetList = convertStringToList(datasetId);
            buildAndSaveQATaskDatasets(qaTask.getId(), taskName, datasetList);
            buildAndSaveQATaskFiles(qaTask.getId(), taskName, fileIdList);

            String taskId = qaTask.getId();
            importQA(taskId, importQAs, userId, datasetId, fileId,fileCount);

            //esService.createQAIndex(qaTask.getId());
            //taskReviewService.addReviewer(userId, qaTask.getId(), userId);

            return BaseResponse.success();

        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return BaseResponse.error();
        }
    }
    private static List<String> convertStringToList(String input) {
        if (input == null || input.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.asList(input.split(","));
    }

    /**
     * 导入本地QA对
     *
     * @param importQAs QA文件
     * @param taskId
     * @return 导入QA对，返回success
     */
    @Transactional(rollbackFor = Exception.class)
    public void importQA(String taskId,MultipartFile importQAs,String userId,String datasetId,String fileId,int fileCount) {
        existsTask(taskId);
        QATask qaTask = this.getById(taskId);
        int originalqacount =  qaTask.getQaCount();
        int totalqacount = esService.saveQAToESfromlocal(userId,taskId, importQAs,datasetId, fileId);
        int newqacount = totalqacount + originalqacount;
        updateQATaskQaCountAndStatus(taskId, newqacount,fileCount);
        updateQATaskFilesQaCountAndStatus(taskId, fileId, totalqacount);
        esService.createQAIndex(taskId);
        saveRecord(taskId,totalqacount);
    }

    private void updateQATaskQaCountAndStatus(String taskId, int qaCount,int fileCount) {
        QATask qaTask = this.getById(taskId);
        long datasetCount = qaTaskDatasetsService.count(new QueryWrapper<QATaskDatasets>().eq("task_id", taskId));
        try {
            lambdaUpdate()
                    .eq(QATask::getId, taskId)
                    .set(QATask::getQaCount, qaCount)
                    .set(QATask::getTotal,fileCount)
                    .set(QATask::getComplete, datasetCount)
                    .update();
            if (datasetCount < fileCount){
                qaTask.setTaskStatus("PROCESSING");
                lambdaUpdate()
                        .eq(QATask::getId, taskId)
                        .set(QATask::getTaskStatus, "PROCESSING")
                        .update();
            }
            else if (datasetCount == fileCount)
            {
                qaTask.setTaskStatus("SUCCESS");
                lambdaUpdate()
                        .eq(QATask::getId, taskId)
                        .set(QATask::getTaskStatus, "SUCCESS")
                        .update();
            }
        } catch (NullPointerException e) {
            log.error("updateQATaskQaCountAndStatus的数量为空: {}", e.getMessage());
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

    public static MultipartFile changeFileExtensionToTxt(MultipartFile file) throws IOException {
        // 获取文件的原始内容
        InputStream inputStream = file.getInputStream();
        String originalContent = IOUtils.toString(inputStream, StandardCharsets.UTF_8);

        // 获取文件名和扩展名
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.endsWith(".json")) {
            throw new IllegalArgumentException("File must have a .json extension");
        }

        // 修改扩展名
        String newFilename = originalFilename.substring(0, originalFilename.lastIndexOf('.')) + ".txt";

        // 创建新的 MultipartFile
        byte[] fileBytes = originalContent.getBytes(StandardCharsets.UTF_8);
        MultipartFile newFile = new ByteArrayMultipartFile(fileBytes, newFilename);

        return newFile;
    }


    public void existsTask(String taskId) {
        QATask qaTask = getById(taskId);
        if (qaTask == null) {
            throw new BusinessException(500, "任务" + taskId + "不存在");
        }
    }

    public String saveRecord(String taskId,int totalqacount) {
        try {
            QATaskLocal qatasklocal = saveQATaskLocal(taskId,totalqacount);
            return String.valueOf(qatasklocal.getId());
        } catch (Exception e) {
            log.error("QA对保存记录失败,原因: {}", e.getMessage());
            throw new BusinessException(500, "QA对保存记录失败");
        }
    }

    private QATaskLocal saveQATaskLocal(String taskId,int totalqacount) {
        LocalDateTime time = LocalDateTime.now();
        QATaskLocal qatasklocal = QATaskLocal.builder()
                .taskId(taskId)
                .totalqaCount(totalqacount)
                .createTime(time)
                .updateTime(time)
                .build();

        int saveResult = qaTaskLocalMapper.insert(qatasklocal);  // 使用 Mapper 插入数据
        if (saveResult <= 0) {
            throw new BusinessException(500, "保存数据集信息失败");
        }

        log.debug("保存数据集 {} 信息记录成功", qatasklocal.getId());
        return qatasklocal;
    }

    private QATask buildAndSaveQATask(String userId, String taskName, TaskConfigMap taskConfigMap, String domain, String description, List<FileInfo> fileInfoList) {
        QATask qaTask = buildQATask(userId, taskName, taskConfigMap, domain, description, fileInfoList.size());

        boolean saveQATaskResult = this.save(qaTask);

        if (!saveQATaskResult) {
            throw new BusinessException(500, "保存任务信息失败");
        }

        log.debug("保存任务信息 {} 成功,任务id为 {}", taskName, qaTask.getId());

        return qaTask;
    }

    public QATask buildAndSavelocalQATask(String userId, String taskName) {
        QATask qaTask = buildlocalQATask(userId, taskName);

        boolean saveQATaskResult = this.save(qaTask);

        if (!saveQATaskResult) {
            throw new BusinessException(500, "保存任务信息失败");
        }

        log.debug("保存任务信息 {} 成功,任务id为 {}", taskName, qaTask.getId());

        return qaTask;
    }

    private QATask buildlocalQATask(String userId, String taskName) {
        LocalDateTime time = LocalDateTime.now();
        return QATask.builder()
                .id(generateQATaskId())
                .taskName(taskName)
                .splitLevel(3)
                .densityLevel(3)
                .domain("")
                .description("")
                .taskStatus("")
                .taskStartTime(time)
                .createTime(time)
                .updateTime(time)
                .taskCreatorId(userId)
                .total(1)
                .complete(0)
                .qaCount(0)
                .reviewCount(0)
                .build();
    }

    public void buildAndSaveQATaskDatasets(String taskId, String taskName, List<String> datasetList) {
        List<QATaskDatasets> qaTaskDatasetsList = buildQATaskDatasetsList(taskId, datasetList);

        boolean saveQATaskFilesResult = qaTaskDatasetsService.saveBatch(qaTaskDatasetsList);

        if (!saveQATaskFilesResult) {
            throw new BusinessException(500, "关联任务与数据集失败");
        }

        log.debug("关联任务 {} 与数据集 {} 成功", taskName, datasetList);
    }

    public void buildAndSaveQATaskFiles(String taskId, String taskName, List<String> fileIdList) {
        List<QATaskFiles> qaTaskFilesList = buildQATaskFilesList(taskId, fileIdList);

        boolean saveQATaskChildFilesResult = qaTaskFilesService.saveBatch(qaTaskFilesList);

        if (!saveQATaskChildFilesResult) {
            throw new BusinessException(500, "关联任务与子文件失败");
        }

        log.info("关联任务 {} 与文件 {} 成功", taskName, fileIdList);
    }


    private QATask buildQATask(String userId, String taskName, TaskConfigMap taskConfigMap, String domain, String description, int fileIdListSize) {
        LocalDateTime time = LocalDateTime.now();
        return QATask.builder()
                .id(generateQATaskId())
                .taskName(taskName)
                .splitLevel(taskConfigMap.getSplitLevel())
                .densityLevel(taskConfigMap.getQuestionDensity())
                .domain(domain)
                .description(description)
                .taskStatus(TaskStatusEnum.PROCESSING.toString())
                .taskStartTime(time)
                .createTime(time)
                .updateTime(time)
                .taskCreatorId(userId)
                .total(fileIdListSize)
                .complete(0)
                .qaCount(0)
                .reviewCount(0)
                .build();
    }

    private List<String> getViewableQATaskIdList(String userId) {
        return taskReviewService.lambdaQuery().eq(TaskReview::getUserId, userId).list()
                .stream().map(TaskReview::getTaskId).collect(Collectors.toList());
    }

    private QATaskListResponse buildQATasksResponses(List<QATask> qaTaskList, Long totalCount) {
        ArrayList<QATaskListResponse.QATaskInfoVO> qaTaskResponseList = new ArrayList<>();
        qaTaskList.forEach(qaTask -> {
            QATaskListResponse.QATaskInfoVO qaTaskResponse = QATaskListResponse.QATaskInfoVO.builder()
                    .taskId(qaTask.getId())
                    .taskName(qaTask.getTaskName())
                    .createTime(qaTask.getCreateTime())
                    .status(qaTask.getTaskStatus())
                    .complete(qaTask.getComplete())
                    .total(qaTask.getTotal())
                    .qaCount(qaTask.getQaCount())
                    .reviewCount(qaTask.getReviewCount())
                    .creator(userService.getUserInfoById(qaTask.getTaskCreatorId()))
                    .reviewers(taskReviewService.getReviewUserList(qaTask.getId(),null))
                    .build();
            qaTaskResponseList.add(qaTaskResponse);
        });
        return QATaskListResponse.builder()
                .qaTaskInfoVOList(qaTaskResponseList)
                .total(totalCount)
                .build();
    }

    private List<String> getFailedFileIdListByTaskId(String taskId) {
        return qaTaskFilesService.lambdaQuery()
                .eq(QATaskFiles::getTaskId, taskId)
                .eq(QATaskFiles::getStatus, QATaskFilesEnum.FAILED)
                .list()
                .stream()
                .map(QATaskFiles::getFileId)
                .collect(Collectors.toList());
    }

    private List<String> getDatasetIdListByTaskId(String taskId) {
        return qaTaskDatasetsService.lambdaQuery().eq(QATaskDatasets::getTaskId, taskId)
                .list().stream().map(QATaskDatasets::getDatasetId).collect(Collectors.toList());
    }

    private FileTreeNode parseFileTreeNode(String tree) {
        try {
            return objectMapper.readValue(tree, FileTreeNode.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException(500, "文件树转换失败");
        }
    }

    private FileTreeNodeForTask buildTreeForTask(FileTreeNode node, String taskId) {
        FileTreeNodeForTask root = new FileTreeNodeForTask(node);
        return traverseTreeForTask(root, taskId);
    }

    private FileTreeNodeForTask traverseTreeForTask(FileTreeNodeForTask node, String taskId) {
        if (node == null) {
            return null;
        }

        int totalQACount = 0;

        CopyOnWriteArrayList<FileTreeNodeForTask> children = node.getChildren();
        if (children != null) {
            CopyOnWriteArrayList<FileTreeNodeForTask> tempChildren = new CopyOnWriteArrayList<>();

            for (FileTreeNodeForTask child : children) {
                if (isDirOrZip(child) || isQATaskFileAndSuccess(taskId, child.getFileId())) {
                    FileTreeNodeForTask processedChild = traverseTreeForTask(child, taskId);
                    totalQACount += processedChild.getQaCount();
                    tempChildren.add(processedChild);
                }
            }

            node.setChildren(tempChildren);
        }

        int currentQACount = updateQACountForNode(node, node.getFileId(), taskId);
        node.setQaCount(currentQACount + totalQACount);

        return node;
    }


    private int updateQACountForNode(FileTreeNodeForTask node, String fileId, String taskId) {
        QATaskFiles qaTaskFiles = qaTaskFilesService.lambdaQuery()
                .eq(QATaskFiles::getTaskId, taskId)
                .eq(QATaskFiles::getFileId, fileId)
                .one();
        int qaCount = 0;
        if (qaTaskFiles != null) {
            qaCount = qaTaskFiles.getQaCount();
        }
        node.setQaCount(qaCount);
        return qaCount;
    }

    public String generateQATaskId() {
        //String pattern = "yyyyMMdd";
        String pattern = "yyyyMMddHHmmss";
        SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);

        // 获取当前日期
        String currentDate = dateFormat.format(new Date());

        // 查询数据库中当前日期中的最大编号
        int currentMaxNumber = this.baseMapper.getCurrentMaxNumber(currentDate);

        // 递增编号
        int newNumber = currentMaxNumber + 1;

        // 构造最终的主键
        return currentDate + String.format("%04d", newNumber);
    }

    public List<QATaskProblemTreeResponse.QATaskProblem> getTreeForProblem(String taskId, List<String> fileIdList) {
        List<QATaskProblemTreeResponse.QATaskProblem> response = new ArrayList<>();
        List<String> datasetIdList = getDatasetIdListByTaskId(taskId);
        for (String datasetId : datasetIdList) {
            DatasetInfo datasetInfo = datasetInfoService.getById(datasetId);

            String tree = datasetInfo.getTree();

            if (StringUtils.isBlank(tree)) {
                continue;
            }
            FileTreeNode curNode = parseFileTreeNode(tree);

            FileTreeNodeForProblem newNode = buildTreeForProblem(curNode, fileIdList);
            response.add(new QATaskProblemTreeResponse.QATaskProblem(datasetId, newNode));
        }

        return response;
    }

    private FileTreeNodeForProblem buildTreeForProblem(FileTreeNode node, List<String> fileIdList) {
        FileTreeNodeForProblem root = new FileTreeNodeForProblem(node);
        return traverseTreeForProblem(root, fileIdList);
    }

    private FileTreeNodeForProblem traverseTreeForProblem(FileTreeNodeForProblem root, List<String> fileIdList) {
        Queue<FileTreeNodeForProblem> queue = new ArrayDeque<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            FileTreeNodeForProblem curNode = queue.poll();
            CopyOnWriteArrayList<FileTreeNodeForProblem> children = curNode.getChildren();

            if (children != null) {
                CopyOnWriteArrayList<FileTreeNodeForProblem> tempChildren = new CopyOnWriteArrayList<>();

                for (FileTreeNodeForProblem child : children) {
                    if (isDirOrZip(child) || isProblemFile(child, fileIdList)) {
                        queue.add(child);
                        tempChildren.add(child);
                    }
                }

                curNode.setChildren(tempChildren);
            }
        }
        return root;
    }

    private boolean isQATaskFileAndSuccess(String taskId, String fileId) {
        return qaTaskFilesService.lambdaQuery()
                .eq(QATaskFiles::getTaskId, taskId)
                .eq(QATaskFiles::getFileId, fileId)
                .eq(QATaskFiles::getStatus, QATaskFilesEnum.SUCCESS)
                .count() > 0;
    }

    private boolean isProblemFile(FileTreeNodeForProblem child, List<String> fileIdList) {
        return fileIdList.contains(child.getFileId());
    }

    private boolean isDirOrZip(FileTreeNodeForProblem child) {
        return child.getType().equals(FileTypeEnum.ZIP) || child.getType().equals(FileTypeEnum.DIR);
    }

    private boolean isDirOrZip(FileTreeNodeForTask child) {
        return child.getType().equals(FileTypeEnum.ZIP) || child.getType().equals(FileTypeEnum.DIR);
    }

    private FileInfo minfile(List<FileInfo> fileInfoList) {
        if (fileInfoList.isEmpty()) {
            return null;
        }
        // 找到最小的 FileInfo
        FileInfo minFileInfo = fileInfoList.get(0);
        for (FileInfo fileInfo : fileInfoList) {
            if (compareFileInfo(fileInfo, minFileInfo) < 0) {
                minFileInfo = fileInfo;
            }
        }
        return minFileInfo;
    }
    private int compareFileInfo(FileInfo file1, FileInfo file2) {
        return Long.compare(file1.getFileSize(), file2.getFileSize());
    }
}
