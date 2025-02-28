package com.xzzn.pollux.service;

import com.xzzn.pollux.common.exception.S3RelatedException;
import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.Item;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class S3Service {
    @Value("${s3.config.bucket}")
    private String bucket;

    @Value("${s3.config.path-prefix}")
    private String pathPrefix;

    @Resource
    private MinioClient client;

    @Value("${s3.config.endpoint}")
    private String endpoint;

    public String uploadObject(String fileId, String pathSuffix, File tmpFile, String fileName) {
        String targetName = buildTargetName(fileId, pathSuffix, tmpFile, fileName);
        ObjectWriteResponse objectWriteResponse;

        try {
            log.debug("开始上传文件 {} 到 {}", tmpFile.getName(), targetName);
            objectWriteResponse = client.uploadObject(
                    UploadObjectArgs.builder()
                            .bucket(bucket)
                            .object(targetName)
                            .filename(tmpFile.getPath())
                            .build()
            );
            log.debug("成功上传文件 {} 到 {}", tmpFile.getName(), targetName);
        } catch (MinioException e) {
            String errMsg = "由于 Minio 异常,无法上传对象 " + fileName;
            log.error(errMsg, e);
            throw new S3RelatedException.MinioException(errMsg + e);
        } catch (IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            String errMsg = "由于 " + e.getClass().getName() + ",无法上传对象 " + fileName;
            log.error(errMsg, e);
            throw new S3RelatedException.UploadObjectException(errMsg + e);
        }

        return endpoint + "/" + bucket + "/" + objectWriteResponse.object();
    }

    private String buildTargetName(String fileId, String pathSuffix, File tmpFile, String fileName) {
        StringBuilder targetName = new StringBuilder(pathPrefix)
                .append(fileId).append("/")
                .append(pathSuffix).append("/");

        if (fileName == null || fileName.isEmpty()) {
            targetName.append(tmpFile.getName());
        } else {
            targetName.append(fileName);
        }

        return targetName.toString();
    }

    public InputStream downloadObject(String object) {
        try {
            log.info("开始下载文件 {}", object);
            return client.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(object)
                    .build());
        } catch (MinioException e) {
            String errMsg = "由于 Minio 异常,无法下载对象 " + object + " due to MinioException";
            log.error(errMsg, e);
            throw new S3RelatedException.MinioException(errMsg);
        } catch (InvalidKeyException | IOException | NoSuchAlgorithmException e) {
            String errMsg = "由于 " + e.getClass().getName() + ",无法下载对象 " + object;
            throw new S3RelatedException.DownloadObjectException(errMsg);
        }
    }

    public void deleteFile(String object) {
        try {
            log.info("Start to remove object {}", object);
            client.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(object)
                            .build()
            );
        } catch (MinioException e) {
            String errMsg = "由于 Minio 异常,无法删除对象 " + object;
            log.error(errMsg, e);
            throw new S3RelatedException.MinioException(errMsg);
        } catch (InvalidKeyException | IOException | NoSuchAlgorithmException e) {
            String errMsg = "由于 " + e.getClass().getName() + ",无法删除对象 " + object;
            throw new S3RelatedException.DeleteObjectException(errMsg);
        }
    }

    public void deleteDirectory(String fileId) {
        StringBuilder targetName = new StringBuilder(pathPrefix)
                .append(fileId).append("/");
        Iterable<Result<Item>> list = client.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucket)
                        .prefix(targetName.toString())
                        .build());
        list.forEach(itemResult -> {
            try {
                client.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(bucket)
                                .object(itemResult.get().objectName())
                                .build());
            } catch (ErrorResponseException | XmlParserException | ServerException | NoSuchAlgorithmException |
                     IOException | InvalidResponseException | InvalidKeyException | InsufficientDataException |
                     InternalException e) {
                String errMsg = "由于 " + e.getClass().getName() + ",无法删除对象 " + targetName;
                throw new S3RelatedException.DeleteObjectException(errMsg);
            }
        });
    }

    public File downloadObjectToFile(String object,String fileName) throws IOException {
        try (InputStream inputStream = client.getObject(GetObjectArgs.builder()
                .bucket(bucket)
                .object(object)
                .build())) {

            // 创建临时文件（确保文件后缀为 .txt）
            Path tempFilePath = Files.createTempFile(fileName, ".txt");

            // 将输入流的内容复制到临时文件中
            Files.copy(inputStream, tempFilePath, StandardCopyOption.REPLACE_EXISTING);

            return tempFilePath.toFile();
        } catch (Exception e) {
            throw new IOException("下载对象失败: " + object, e);
        }
    }

    public List<File> downloadAllFilesInDirectory(String prefix) throws IOException {
        List<File> downloadedFiles = new ArrayList<>();

        try {
            Iterable<Result<Item>> objects = client.listObjects(ListObjectsArgs.builder()
                    .bucket(bucket)
                    .prefix(prefix)
                    .build());

            for (Result<Item> result : objects) {
                Item item = result.get();
                String objectName = item.objectName();

                // 如果是文件，下载文件
                if (!objectName.endsWith("/")) {
                    File downloadedFile = downloadObjectToFile(objectName,"爬虫文件-");
                    downloadedFiles.add(downloadedFile);
                }
            }
        } catch (Exception e) {
            throw new IOException("获取目录对象失败: " + prefix, e);
        }

        return downloadedFiles;
    }


}
