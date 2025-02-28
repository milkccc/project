package com.xzzn.pollux.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xzzn.pollux.common.enums.DatasetStatusEnum;
import com.xzzn.pollux.common.enums.FileStatusEnum;
import com.xzzn.pollux.common.enums.OCRTaskStatusEnum;
import com.xzzn.pollux.common.exception.FileProcessingException;
import com.xzzn.pollux.common.exception.S3RelatedException;
import com.xzzn.pollux.entity.DatasetInfo;
import com.xzzn.pollux.entity.FileInfo;
import com.xzzn.pollux.mapper.DatasetInfoMapper;
import com.xzzn.pollux.model.vo.request.ocr.OCRFileParseRequest;
import com.xzzn.pollux.service.OCRService;
import com.xzzn.pollux.service.S3Service;
import com.xzzn.pollux.service.impl.DatasetInfoServiceImpl;
import com.xzzn.pollux.service.impl.FileInfoServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

@Slf4j
@Component
public class FileParseMqListener {

    @Resource
    private OCRService ocrService;

    @Resource
    private S3Service s3Service;

    @Resource
    private FileInfoServiceImpl fileInfoService;

    @Resource
    private DatasetInfoServiceImpl datasetInfoService;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private DatasetInfoMapper datasetInfoMapper;

    @RabbitListener(queues = "file.parse.queue", concurrency = "1")
    public void listenFileParseQueue(String message) {
        try {
            log.info("队列 file.parse.queue收到消息 {}", message);

            OCRFileParseRequest request = objectMapper.readValue(message, OCRFileParseRequest.class);

            String fileId = request.getFileId();
            String filePath = request.getFilePath();
            String datasetId = request.getDatasetId();
            String pathSuffix = request.getPathSuffix();

            uploadFile(fileId, filePath);
            createTask(fileId);
            listenTaskStatus(fileId, pathSuffix, datasetId);
        } catch (FileProcessingException e) {
            log.error("OCR解析文件 {} 错误: {}", e.getFileId(), e.getMessage());
            setDatasetAndFileFailed(e.getFileId(), "OCR解析文件错误:" + e.getMessage());
        } catch (JsonProcessingException e) {
            log.error("监听器转换消息错误: {}", e.getMessage());
        }

    }

//    private void uploadFile(String fileId, String filePath) throws FileProcessingException {
//        if (!ocrService.uploadFileToDGP(new File(filePath), fileId)) {
//            throw new FileProcessingException("OCR上传文件失败", fileId);
//        }
//    }

    private void uploadFile(String fileId, String fileUrl) throws FileProcessingException {
        File tempFile = null;
        String fileName = extractFileNameWithoutExtension(fileUrl);
        try {
            // 创建临时文件
            tempFile = File.createTempFile(fileName, ".pdf");
            log.info("Temporary file created at: {}", tempFile.getAbsolutePath());
            URL url = new URL(fileUrl);

            // 下载文件到本地临时文件
            try (InputStream in = url.openStream()) {
                Files.copy(in, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            // 上传本地临时文件到 OCR 服务
            if (!ocrService.uploadFileToDGP(tempFile, fileId)) {
                throw new FileProcessingException("OCR上传文件失败", fileId);
            }
        } catch (IOException e) {
            throw new FileProcessingException("文件下载或处理失败",  fileId);
        } finally {
            // 删除临时文件
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    public String extractFileNameWithoutExtension(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty");
        }

        // 找到最后一个斜杠的位置
        int lastSlashIndex = filePath.lastIndexOf('/');
        if (lastSlashIndex == -1 || lastSlashIndex == filePath.length() - 1) {
            throw new IllegalArgumentException("Invalid file path format");
        }

        // 提取最后一个斜杠之后的部分
        String fileNameWithExtension = filePath.substring(lastSlashIndex + 1);

        // 找到最后一个点的位置
        int lastDotIndex = fileNameWithExtension.lastIndexOf('.');
        if (lastDotIndex == -1) {
            // 如果没有找到点，就返回整个字符串
            return fileNameWithExtension;
        }

        // 返回点之前的部分，也就是文件名（不包含扩展名）
        return fileNameWithExtension.substring(0, lastDotIndex);
    }



    private void createTask(String fileId) throws FileProcessingException {
        if (!ocrService.createParseTask(fileId)) {
            throw new FileProcessingException("创建解析任务失败", fileId);
        }

    }

    private void listenTaskStatus(String fileId, String pathSuffix, String datasetId) throws FileProcessingException {
        while (true) {
            OCRTaskStatusEnum status = ocrService.listenTaskStatus(fileId, "MD");
            log.debug("解析任务状态为 {}", status);
            if (OCRTaskStatusEnum.ERROR.equals(status)) {
                throw new FileProcessingException("文件解析失败", fileId);
            } else if (OCRTaskStatusEnum.FINISH.equals(status)) {
                downloadFile(fileId, pathSuffix, datasetId);
                break;
            } else {
                sleep(3000);
            }
        }
    }

    private void downloadFile(String fileId, String pathSuffix, String datasetId) throws FileProcessingException {
        pathSuffix = changePathSuffix(pathSuffix);
        File file = ocrService.downloadParsedFile(datasetId, fileId, pathSuffix, "MD");
        if (file == null) {
            throw new FileProcessingException(fileId, "下载解析文件错误");
        }

        String s3Path;
        try {
            s3Path = s3Service.uploadObject(datasetId, pathSuffix, file, file.getName());
        } catch (S3RelatedException.MinioException | S3RelatedException.UploadObjectException e) {
            throw new FileProcessingException("S3相关异常：" + e.getMessage(), fileId);
        }
        updateFileAndDatasetInfo(fileId, s3Path);
    }

    private void updateFileAndDatasetInfo(String fileId, String s3Path) throws FileProcessingException {
        try {
            fileInfoService.lambdaUpdate()
                    .eq(FileInfo::getId, fileId)
                    .set(FileInfo::getParseFilePath, s3Path)
                    .set(FileInfo::getFileStatus, DatasetStatusEnum.SUCCESS)
                    .update();
            FileInfo fileInfo = fileInfoService.getById(fileId);
            String datasetId = fileInfo.getDatasetId();

            datasetInfoMapper.incrementComplete(datasetId);
            datasetInfoMapper.updateStatus(datasetId);
        } catch (Exception e) {
            throw new FileProcessingException("更新文件对应数据库信息失败", fileId);
        }
    }

    private String changePathSuffix(String pathSuffix) {
        if (pathSuffix.startsWith("raw")) {
            return "parse" + pathSuffix.substring(3);
        }
        return pathSuffix;
    }

    private void setDatasetAndFileFailed(String fileId, String failReason) {
        fileInfoService.lambdaUpdate()
                .eq(FileInfo::getId, fileId)
                .set(FileInfo::getFileStatus, FileStatusEnum.FAILED)
                .set(FileInfo::getFailReason, failReason)
                .update();
        FileInfo fileInfo = fileInfoService.getById(fileId);
        datasetInfoService.lambdaUpdate()
                .eq(DatasetInfo::getId, fileInfo.getDatasetId())
                .set(DatasetInfo::getDatasetStatus, DatasetStatusEnum.ERROR)
                .update();
    }

    public void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}