package com.xzzn.pollux.listener;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xzzn.pollux.common.enums.DatasetStatusEnum;
import com.xzzn.pollux.common.enums.FileStatusEnum;
import com.xzzn.pollux.common.exception.FileProcessingException;
import com.xzzn.pollux.common.exception.FileRelatedException;
import com.xzzn.pollux.entity.DatasetInfo;
import com.xzzn.pollux.entity.FileInfo;
import com.xzzn.pollux.mapper.DatasetInfoMapper;
import com.xzzn.pollux.model.pojo.FileParseRequest;
import com.xzzn.pollux.service.S3Service;
import com.xzzn.pollux.service.impl.DatasetInfoServiceImpl;
import com.xzzn.pollux.service.impl.FileInfoServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.poi.extractor.POITextExtractor;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.*;
import java.net.URL;

import static com.xzzn.pollux.utils.FileUtils.DOC;

@Slf4j
@Component
public class CsvFileParseMqListener {
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


    @RabbitListener(queues = "file.parse.csv.queue", concurrency = "1")
    public void listenWordFileParseQueue(String message) {
        try {
            log.info("队列 file.parse.csv.queue收到消息 {}", message);
            FileParseRequest fileParseRequest = objectMapper.readValue(message, FileParseRequest.class);
            String datasetId = fileParseRequest.getDatasetId();
            String fileId = fileParseRequest.getFileId();
            String fileName = fileParseRequest.getFileName();
            String filePath = fileParseRequest.getFilePath();
            log.info("fileParseRequest:{}", fileParseRequest);
            parseFile(datasetId, fileId, fileName, filePath);
        } catch (FileProcessingException e) {
            log.error("解析csv文件 {} 错误: {}", e.getFileId(), e.getMessage());
            setDatasetAndFileFailed(e.getFileId(), "解析csv文件错误:" + e.getMessage());
        }
        catch (JsonProcessingException e) {
            log.error("监听器转换消息错误: {}", e.getMessage());
        }
    }

//    public void parseFile(String datasetId, String fileId, String fileName, String filePath) throws FileProcessingException {
//        try {
//            String mdFileName = fileName + ".md";
//            File file = new File(filePath);
//
//            File mdTempFile = File.createTempFile(fileName, ".md");
//            BufferedReader reader = new BufferedReader(new FileReader(file));
//            FileWriter writer = new FileWriter(mdTempFile);
//            String line;
//
//            StringBuffer markdownTable = new StringBuffer();
//            boolean isfirstline = true;
//
//            while ((line = reader.readLine()) != null) {
//                // 逐行读取 CSV 文件内容
//                String[] columns = line.split(","); // 假设 CSV 文件使用逗号分隔
//
//                StringBuffer markdownRow = new StringBuffer(" | ");
//
//                // 处理每一行的数据
//                for (String column : columns) {
//                    markdownRow.append(column).append(" | ");
//                }
//                markdownRow.append("\n");
//                markdownTable.append(markdownRow);
//
//                if (isfirstline) {
//                    StringBuffer splitRow = new StringBuffer(" | ");
//                    for (int i = 0; i< columns.length; i++) {
//                        splitRow.append("-----").append(" | ");
//                    }
//                    splitRow.append("\n");
//                    markdownTable.append(splitRow);
//                }
//
//                isfirstline = false;
//            }
//            writer.write(markdownTable.toString());
//
//            writer.close();
//            reader.close();
//
//            String s3Path = s3Service.uploadObject(datasetId, "parse", mdTempFile, mdFileName);
//
//            updateFileAndDatasetInfo(fileId, s3Path);
//            log.debug("csv文件{}解析完成", fileName);
//        } catch (Exception e) {
//            throw new FileProcessingException("文件解析失败", fileId);
//        }
//    }
public void parseFile(String datasetId, String fileId, String fileName, String filePath) throws FileProcessingException {
    BufferedReader reader = null;
    FileWriter writer = null;
    File tempFile = null;
    File mdTempFile = null;

    try {
        log.info("处理文件名以确保其合法");
        String safeFileName = "temp_" + System.currentTimeMillis(); // 使用时间戳确保唯一性

        // 从 URL 下载文件到本地临时文件
        URL url = new URL(filePath);
        tempFile = File.createTempFile(safeFileName, ".tmp");
        try (InputStream in = url.openStream(); FileOutputStream fos = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        }

        log.info("准备解析文件");
        String mdFileName = safeFileName + ".md";
        mdTempFile = File.createTempFile(safeFileName, ".md");
        reader = new BufferedReader(new FileReader(tempFile));
        writer = new FileWriter(mdTempFile);

        String line;
        StringBuffer markdownTable = new StringBuffer();
        boolean isfirstline = true;

        while ((line = reader.readLine()) != null) {
            // 逐行读取 CSV 文件内容
            String[] columns = line.split(","); // 假设 CSV 文件使用逗号分隔
            StringBuffer markdownRow = new StringBuffer(" | ");

            // 处理每一行的数据
            for (String column : columns) {
                markdownRow.append(column).append(" | ");
            }
            markdownRow.append("\n");
            markdownTable.append(markdownRow);

            if (isfirstline) {
                StringBuffer splitRow = new StringBuffer(" | ");
                for (int i = 0; i < columns.length; i++) {
                    splitRow.append("-----").append(" | ");
                }
                splitRow.append("\n");
                markdownTable.append(splitRow);
            }

            isfirstline = false;
        }

        writer.write(markdownTable.toString());

        String s3Path = s3Service.uploadObject(datasetId, "parse", mdTempFile, mdFileName);
        updateFileAndDatasetInfo(fileId, s3Path);
        log.debug("csv文件{}解析完成", fileName);
    } catch (IOException e) {
        log.error("文件IO异常: {}", e.getMessage());
        throw new FileProcessingException("文件读取失败", fileId);
    } catch (Exception e) {
        log.error("未知异常: {}", e.getMessage());
        throw new FileProcessingException("文件解析失败", fileId);
    } finally {
        // 关闭资源并删除临时文件
        try {
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (tempFile != null && tempFile.exists()) tempFile.delete();
            if (mdTempFile != null && mdTempFile.exists()) mdTempFile.delete();
        } catch (IOException e) {
            log.error("关闭文件流失败: {}", e.getMessage());
        }
    }
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
}
