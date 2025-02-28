package com.xzzn.pollux.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xzzn.pollux.common.ResultResponse;
import com.xzzn.pollux.common.exception.FileProcessingException;
import com.xzzn.pollux.service.S3Service;
import com.xzzn.pollux.utils.CustomMultipartFile;
import com.xzzn.pollux.mapper.FileInfoMapper;
import com.xzzn.pollux.model.pojo.AutogenRequest;
import com.xzzn.pollux.model.pojo.TaskConfigMap;
import com.xzzn.pollux.model.vo.request.task.QATaskAutoCreateRequest;
import com.xzzn.pollux.service.impl.DatasetInfoServiceImpl;
import com.xzzn.pollux.service.impl.QATaskServiceImpl;
import com.xzzn.pollux.utils.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.io.File;

@Slf4j
@Component
public class AutogenTaskMqListener {
    @Resource
    private DatasetInfoServiceImpl datasetInfoService;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private FileInfoMapper fileInfoMapper;

    @Resource
    private QATaskServiceImpl qataskService;

    @Resource
    private  QATaskAutoCreateRequest qaTaskAutoCreateRequest;

    @Resource
    private S3Service s3Service;

    private static final Random random = new Random();


    @RabbitListener(queues = "crawler.file.txt.queue", concurrency = "1")
    public void listenCrawlerFileQueue(String message) {
        try {
            log.info("队列 crawler.file.txt.queue收到消息 {}", message);

            AutogenRequest autogenRequest = objectMapper.readValue(message, AutogenRequest.class);
            String datasetId = autogenRequest.getDatasetId();
            String fileId = autogenRequest.getFileId();
            String fileName = autogenRequest.getFileName();
            String filePath = autogenRequest.getFilePath();
            log.debug("爬取文件id {}, 文件路径{}", fileId, filePath);
            if (filePath!=null && !"".equals(filePath)) {
                autogenTask(fileName, filePath);
            }

        }
        catch (FileProcessingException e) {
            log.error("自动生成任务未完成 {} 错误: {}", e.getFileId(), e.getMessage());
        }
        catch (JsonProcessingException e) {
            log.error("监听器转换消息错误: {}", e.getMessage());
        }

    }
    public void autogenTask(String fileName, String filePath) throws FileProcessingException {
        try {
            // 从 URL 提取对象名称
            String objectName = extractObjectNameFromUrl(filePath);

            List<File> Files = null;
            int retryCount = 100;
            int retryInterval = 1000; // 1 秒
            for (int i = 0; i < retryCount; i++) {
                Files = s3Service.downloadAllFilesInDirectory(objectName);
                if (Files != null) {
                    break;
                }
                Thread.sleep(retryInterval);
            }
            if (Files.isEmpty()) {
                log.error("No txt files found at the URL: " , filePath);
            }
            String userId = "9fa41311373495e654bfcce77d567d51";
            //String taskName = qaTaskAutoCreateRequest.getTaskName();
            String taskName = "自动任务-" + generateQATaskId();
            TaskConfigMap taskConfigMap = qaTaskAutoCreateRequest.getTaskConfigMap();
            String domain = qaTaskAutoCreateRequest.getDomain();
            String description = qaTaskAutoCreateRequest.getDescription();
            boolean priority = qaTaskAutoCreateRequest.isPriority();
            generatetask(Files,userId,taskName,taskConfigMap,domain,description,priority,objectName);
        } catch (InterruptedException e) {
            log.error("Thread was interrupted during retries: " + e.getMessage(), e);
            Thread.currentThread().interrupt(); // 恢复中断状态
        } catch (IllegalArgumentException e) {
            log.error("Invalid argument: " + e.getMessage(), e);
        } catch (NullPointerException e) {
            log.error("Null pointer encountered: " + e.getMessage(), e);
        }
        catch (Exception e) {
                log.error("Error in autogenTask: {}" , e.getMessage() );
        }
    }


    public CompletableFuture<ResultResponse<String>> generatetask(List<File> files,String userId, String taskName,
                                                                      TaskConfigMap taskConfigMap, String domain,
                                                                  String description, boolean priority,String objectName) throws InterruptedException {

        List<String> datasetList = new ArrayList<>();
        uploaddata(files,userId,datasetList);
        String taskId = qataskService.createQATask(userId, taskName, datasetList, taskConfigMap, domain, description, false);
        Thread.sleep(1000);
        if(taskId != null) {
            s3Service.deleteFile(objectName);
            log.info("爬虫临时文件{}已删除",objectName);
        }
        return CompletableFuture.completedFuture(ResultUtils.success(taskId));
    }


    public void uploaddata(List<File> files, String userId, List<String> datasetList) {
        for (File file : files) {
                try {
                    MultipartFile multipartFile = new CustomMultipartFile(file);

                    CompletableFuture<String> datasetIdFuture = datasetInfoService.generateDataSetlocalQA(userId, multipartFile, null);
                    String datasetId = null;
                    String fileId = null;
                    int retryCount = 60;
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

                    // 将生成的 datasetId 添加到列表中
                    if (datasetId != null && !datasetId.isEmpty()) {
                        datasetList.add(datasetId);
                        log.info("数据集上传完成，数据集ID：{}", datasetId);
                    } else {
                        log.error("Failed to generate datasetId for file: " , file.getName());
                    }

                } catch (IOException | ExecutionException | InterruptedException e) {
                    log.error("Error processing file: " , file.getName());
                    e.printStackTrace();
                }

        }

    }

    private String extractObjectNameFromUrl(String url) {
        // 实现提取对象名称的逻辑
        return url.split("static-pollux/")[1];
    }

    public String generateQATaskId() {
        // 定义日期格式
        String pattern = "yyyyMMddHHmmss";
        SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);

        // 获取当前日期时间
        String currentDate = dateFormat.format(new Date());

        // 递增编号
        int newNumber = 1000 + random.nextInt(9000);

        // 构造最终的主键
        return currentDate + String.format("%04d", newNumber);
    }

}
