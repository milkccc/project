package com.xzzn.pollux.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xzzn.pollux.common.enums.OCRTaskStatusEnum;
import com.xzzn.pollux.model.vo.response.ocr.OCRResponse;
import com.xzzn.pollux.model.vo.response.ocr.OCRTaskResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
@Slf4j
public class OCRService {

    @Value("${file.parse.url}")
    private String fileParseUrlPrefix;

    @Value("${file.parse.token}")
    private String fileParseToken;

    @Value("${file.parse.authorization}")
    private String fileParseAuthorization;

    @Resource
    private RestTemplate restTemplate;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private S3Service s3Service;


    /**
     * 上传待处理文件到DGP平台
     *
     * @param file   待处理文件
     * @param fileId 文件id
     * @return 返回上传文件状态
     */
    public boolean uploadFileToDGP(File file, String fileId) {
        log.debug("上传待解析文件 {}", fileId);
        String url = fileParseUrlPrefix + "/files";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set(fileParseAuthorization, fileParseToken);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(file));
        body.add("file_id", fileId);
        body.add("folder_id", "pollux");

        try {
            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

            OCRResponse ocrResponse = handleOCRHttpResponse(response);

            log.debug("上传完成,状态:{},{}", ocrResponse.getCode(), ocrResponse.getMsg());
            return "200".equals(ocrResponse.getCode());
        } catch (Exception e) {
            log.error("上传待解析文件 {} 异常: ", fileId, e);
            return false;
        }
    }

    /**
     * 创建解析任务
     *
     * @param fileId 文件id
     * @return 返回创建任务状态
     */
    public boolean createParseTask(String fileId) {
        log.debug("创建解析任务 {}", fileId);
        String url = fileParseUrlPrefix + "/task";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(fileParseAuthorization, fileParseToken);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("file_ids", new String[]{fileId});
        requestBody.put("target_file", "MD");

        try {
            String body = objectMapper.writeValueAsString(requestBody);

            HttpEntity<String> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

            OCRResponse ocrResponse = handleOCRHttpResponse(response);

            log.debug("创建解析任务完成,状态:{},{}", ocrResponse.getCode(), ocrResponse.getMsg());
            return "200".equals(ocrResponse.getCode());
        } catch (Exception e) {
            log.error("创建解析任务 {} 异常: ", fileId, e);
            return false;
        }
    }

    /**
     * 监听任务状态
     *
     * @param fileId     文件id
     * @param targetFile 目标类型
     * @return 返回监听状态
     */
    public OCRTaskStatusEnum listenTaskStatus(String fileId, String targetFile) {
        log.debug("监听任务 {} 状态", fileId);
        String url = fileParseUrlPrefix + "/task/status";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set(fileParseAuthorization, fileParseToken);

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("file_id", fileId)
                .queryParam("target_file", targetFile);

        String queryUrl = builder.toUriString();

        try {
            HttpEntity<Object> request = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(queryUrl, HttpMethod.GET, request, String.class);

            OCRTaskResponse ocrResponse = objectMapper.readValue(response.getBody(), OCRTaskResponse.class);

            log.debug("监听任务中,状态:{}, {}", ocrResponse.getCode(), ocrResponse.getMsg());

            return OCRTaskStatusEnum.valueOf(ocrResponse.getData().getTaskStatus());
        } catch (Exception e) {
            log.error("监听任务 {} 错误: ", fileId, e);
            return OCRTaskStatusEnum.ERROR;
        }
    }

    /**
     * 下载解析结果
     *
     * @param fileId     文件id
     * @param exportType 导出类型
     * @return 返回下载文件
     */
    public File downloadParsedFile(String datasetId, String fileId, String pathSuffix, String exportType) {
        log.debug("下载解析文件 {}", fileId);
        String url = fileParseUrlPrefix + "/task/download";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(fileParseAuthorization, fileParseToken);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("file_ids", new String[]{fileId});
        requestBody.put("export_type", exportType);

        String body;
        try {
            body = objectMapper.writeValueAsString(requestBody);
            HttpEntity<String> request = new HttpEntity<>(body, headers);
            ResponseEntity<byte[]> responseEntity = restTemplate.exchange(url, HttpMethod.POST, request, byte[].class);

            return saveAndUnzip(datasetId, pathSuffix, responseEntity.getBody(), fileId + ".zip");
        } catch (Exception e) {
            log.error("下载解析文件 {} 错误: ", fileId, e);
            return null;
        }
    }

    public OCRResponse handleOCRHttpResponse(ResponseEntity<String> response) throws JsonProcessingException {
        return objectMapper.readValue(response.getBody(), OCRResponse.class);
    }


    private File saveAndUnzip(String datasetId, String pathSuffix, byte[] zipBytes, String fileName) {
        try {
            Path tempDirectory = Files.createTempDirectory("tempDir");
            Path tempFile = tempDirectory.resolve(fileName);
            Files.write(tempFile, zipBytes);

            return unzip(datasetId, pathSuffix, tempFile.toFile(), tempDirectory.toFile());
        } catch (IOException e) {
            log.error("下载的解析文件错误：", e);
            return null;
        }
    }

    private File unzip(String datasetId, String pathSuffix, File zipFile, File outputDirectory) throws IOException {
        Map<String, String> imageMap = new HashMap<>(); // 用于存储本地图片路径和MinIO URL的映射
        File mdFile = null;

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile.toPath()))) {
            ZipEntry zipEntry;
            while ((zipEntry = zis.getNextEntry()) != null) {
                File outputFile = new File(outputDirectory, zipEntry.getName());
                if (zipEntry.isDirectory()) {
                    outputFile.mkdirs();
                } else {
                    try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        while ((bytesRead = zis.read(buffer)) != -1) {
                            fos.write(buffer, 0, bytesRead);
                        }
                    }

                    if (outputFile.getName().endsWith(".md")) {
                        mdFile = outputFile;
                    }

                    if (outputFile.getName().endsWith(".png")) {
                        String imageUrl = s3Service.uploadObject(datasetId, pathSuffix, outputFile, zipEntry.getName());
                        imageMap.put(zipEntry.getName(), imageUrl);
                    }

                }
            }
        }
        if (mdFile == null) {
            return null;
        }
        replaceImagePathsInMarkdown(mdFile.toPath(), imageMap);
        return mdFile;
    }

    private void replaceImagePathsInMarkdown(Path mdPath, Map<String, String> imageMap) throws IOException {
        String mdContent = new String(Files.readAllBytes(mdPath), StandardCharsets.UTF_8);

        // 替换Markdown文件中的图片路径
        for (Map.Entry<String, String> entry : imageMap.entrySet()) {
            String imageName = entry.getKey();
            String minioUrl = entry.getValue();
            mdContent = mdContent.replaceAll(Pattern.quote(imageName), Matcher.quoteReplacement(minioUrl));
        }

        // 写回修改后的MD内容
        Files.write(mdPath, mdContent.getBytes(StandardCharsets.UTF_8));
    }
}