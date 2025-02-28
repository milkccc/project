package com.xzzn.pollux.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xzzn.pollux.common.enums.DatasetStatusEnum;
import com.xzzn.pollux.common.enums.FileStatusEnum;
import com.xzzn.pollux.common.enums.FileTypeEnum;
import com.xzzn.pollux.common.exception.BusinessException;
import com.xzzn.pollux.entity.DatasetInfo;
import com.xzzn.pollux.entity.FileInfo;
import com.xzzn.pollux.entity.QATaskDatasets;
import com.xzzn.pollux.mapper.DatasetInfoMapper;
import com.xzzn.pollux.model.pojo.FileParseRequest;
import com.xzzn.pollux.model.pojo.FileTreeNode;
import com.xzzn.pollux.model.vo.request.ocr.OCRFileParseRequest;
import com.xzzn.pollux.model.vo.response.dataset.DatasetListResponse;
import com.xzzn.pollux.model.vo.response.dataset.FilePreviewResponse;
import com.xzzn.pollux.service.IDatasetInfoService;
import com.xzzn.pollux.service.S3Service;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.*;
import java.nio.file.Files;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.xzzn.pollux.utils.FileUtils.*;

/**
 * <p>
 * 数据集信息表 服务实现类
 * </p>
 *
 * @author xzzn
 */
@Service
@Slf4j
public class DatasetInfoServiceImpl extends ServiceImpl<DatasetInfoMapper, DatasetInfo> implements IDatasetInfoService {

    @Resource
    private AsyncTaskExecutor fileUploadTaskExecutor;
    @Resource
    private S3Service s3Service;

    @Resource
    private FileInfoServiceImpl fileInfoService;

    @Resource
    private QATaskDatasetsServiceImpl qaTaskDatasetsService;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    private ObjectMapper objectMapper;

    @Value("${file.parse.exchange}")
    private String fileParseExchange;

    @Value("${file.parse.queue.rk}")
    private String fileParseQueueRoutingKey;

    @Autowired
    private DatasetInfoMapper datasetInfoMapper;

    private final Random random = new Random();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String generateDataSet(String userId, MultipartFile datasetFile, List<String> tags) {
        String datasetName = datasetFile.getOriginalFilename();

        log.debug("用户 {} 开始导入数据集 {}", userId, datasetName);

        try {
            File tempFile = saveMultipartFileToTempFile(datasetFile, datasetName);

            String datasetId = saveRecord(userId, tempFile, datasetName, tags);

            uploadDataset(datasetId, datasetName, tempFile);
            log.debug("用户 {} 导入数据集 {} 成功", userId, datasetName);

            fileUploadTaskExecutor.submit(() -> asyncUnZipAndUploadAndProcess(datasetId, datasetName, tempFile));

            return datasetId;
        } catch (IOException ioException) {
            log.error("导入数据集 {} 失败,原因: {}", datasetName, ioException.getStackTrace());
            throw new BusinessException(500, "导入数据集失败");
        }
    }

    public CompletableFuture<String> generateDataSetlocalQA(String userId, MultipartFile datasetFile, List<String> tags) {
        String datasetName = datasetFile.getOriginalFilename();
        log.debug("用户 {} 开始导入数据集 {}", userId, datasetName);

        return CompletableFuture.supplyAsync(() -> {
            try {
                File tempFile = saveMultipartFileToTempFile(datasetFile, datasetName);
                String datasetId = saveRecord(userId, tempFile, datasetName, tags);
                uploadDataset(datasetId, datasetName, tempFile);
                log.debug("用户 {} 导入数据集 {} 成功", userId, datasetName);

                fileUploadTaskExecutor.submit(() -> asyncUnZipAndUploadAndProcess(datasetId, datasetName, tempFile));

                return datasetId;
            } catch (IOException ioException) {
                log.error("导入数据集 {} 失败,原因: {}", datasetName, ioException.getMessage());
                throw new BusinessException(500, "导入数据集失败");
            }
        });
    }


    public List<String> getLatestTwoDatasetIds() {
        QueryWrapper<DatasetInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("create_time").last("LIMIT 1");
        List<DatasetInfo> latestDatasets = datasetInfoMapper.selectList(queryWrapper);
        List<String> latestIds = latestDatasets.stream()
                .map(DatasetInfo::getId)
                .collect(Collectors.toList());
        return latestIds;
    }

    public List<String> getSecondAndThirdDatasetIds() {
        QueryWrapper<DatasetInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("create_time").last("LIMIT 2 OFFSET 1");
        List<DatasetInfo> secondAndThirdDatasets = datasetInfoMapper.selectList(queryWrapper);
        List<String> ids = secondAndThirdDatasets.stream()
                .map(DatasetInfo::getId)
                .collect(Collectors.toList());
        return ids;
    }



    private File saveMultipartFileToTempFile(MultipartFile datasetFile, String datasetName) throws IOException {
        String ext = FilenameUtils.getExtension(datasetName).toLowerCase();
        File tempFile;

        // 处理非zip文件,转换为zip文件统一处理
        if (!"zip".equalsIgnoreCase(ext)) {
            tempFile = File.createTempFile(FilenameUtils.getBaseName(datasetName), ".zip");
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(tempFile.toPath()))) {
                zipOutputStream.putNextEntry(new ZipEntry(datasetName));
                zipOutputStream.write(datasetFile.getBytes());
                zipOutputStream.closeEntry();
            } catch (IOException e) {
                throw new IOException("创建压缩文件失败", e);
            }
        } else {
            tempFile = File.createTempFile(FilenameUtils.getBaseName(datasetName), "." + ext);
            datasetFile.transferTo(tempFile);
        }

        log.debug("原始数据集 {} 保存为临时文件 {}", datasetName, tempFile.getName());
        return tempFile;
    }

    private String saveRecord(String userId, File tempFile, String datasetName, List<String> tags) {
        try {
            DatasetInfo datasetInfo = saveDatasetInfo(userId, tempFile, datasetName, tags);
            return datasetInfo.getId();
        } catch (Exception e) {
            log.error("数据集 {} 保存记录失败,原因: {}", datasetName, e.getMessage());
            throw new BusinessException(500, "数据集保存记录失败");
        }
    }

    private DatasetInfo saveDatasetInfo(String userId, File tmpFile, String datasetName, List<String> tags) {
        LocalDateTime time = new Timestamp(System.currentTimeMillis()).toLocalDateTime();
        DatasetInfo datasetInfo;
        datasetInfo = DatasetInfo.builder()
                .datasetName(datasetName)
                .uploaderId(userId)
                .datasetSize(tmpFile.length() / 1024)
                .datasetStatus(DatasetStatusEnum.UPLOADING.toString())
                .tags(processTags(tags))
                .tree("")
                .createTime(time)
                .updateTime(time)
                .build();

        boolean saveResult = this.save(datasetInfo);
        if (!saveResult) {
            throw new BusinessException(500, "保存数据集信息失败");
        }

        log.debug("保存数据集 {} 信息记录成功", datasetInfo.getId());
        return datasetInfo;
    }

    private String processTags(List<String> tags) {
        if (tags == null) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(tags);
        } catch (JsonProcessingException e) {
            throw new BusinessException(500, "数据集标签转储JSON错误");
        }
    }

    private void uploadDataset(String datasetId, String datasetName, File tempFile) {
        log.debug("上传数据集 {} ", datasetName);
        try {
            String s3Path = uploadDatasetToS3(datasetId, datasetName, tempFile);

            this.lambdaUpdate()
                    .eq(DatasetInfo::getId, datasetId)
                    .set(DatasetInfo::getDatasetPath, s3Path)
                    .set(DatasetInfo::getDatasetStatus, DatasetStatusEnum.PARSING)
                    .update();
        } catch (Exception e) {
            log.error("数据集 {} 上传S3失败,原因: {}", datasetName, e.getMessage());
            throw new BusinessException(500, "数据集上传失败");
        }
    }

    private String uploadDatasetToS3(String datasetId, String datasetName, File tmpFile) {
        return s3Service.uploadObject(datasetId, "raw", tmpFile, datasetName);
    }

    public void asyncUnZipAndUploadAndProcess(String datasetId, String datasetName, File tempFile) {
        FileTreeNode root = new FileTreeNode(datasetName, FileTypeEnum.ZIP);

        unzipAndTraverseFolder(tempFile, datasetId, datasetName, root, "raw", 0);

        handlePostTraversal(datasetId, root);
    }

    private void unzipAndTraverseFolder(File tempFile, String datasetId, String fileName, FileTreeNode root, String pathSuffix, int fileLevel) {
        try {
            String destDirPath = createDestinationDirectory(datasetId, fileName);

            unZip(tempFile, destDirPath);

            traverseFolder(datasetId, pathSuffix, new File(destDirPath), fileLevel + 1, root);
        } catch (Exception e) {
            updateDatasetErrorStatus(datasetId);
            log.error("解压 {} 失败,原因: {}", datasetId, e.getMessage());
        }
    }

    public void traverseFolder(String datasetId, String pathSuffix, File folder, int fileLevel, FileTreeNode root) {
        if (shouldTraverseFolder(folder, fileLevel)) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    processFileOrFolder(datasetId, pathSuffix, file, fileLevel, root);
                }
            }
        }
    }

    private String createDestinationDirectory(String datasetId, String datasetName) {
        StringBuilder destDirPathBuilder = new StringBuilder(System.getProperty("java.io.tmpdir"));
        if (!destDirPathBuilder.toString().endsWith("/")) {
            destDirPathBuilder.append("/");
        }
        return destDirPathBuilder.append(datasetId).append("/")
                .append(FilenameUtils.getBaseName(datasetName))
                .append(random.nextLong()).append("/").toString();
    }

    private void handlePostTraversal(String datasetId, FileTreeNode root) {
        generateFileTree(datasetId, root);
        int complete = countCompletedFiles(datasetId);
        int total = countTotalFiles(root);

        String status = DatasetStatusEnum.PARSING.toString();
        if (total == complete) {
            status = DatasetStatusEnum.SUCCESS.toString();
        }

        updateDatasetStatus(datasetId, complete, total, status);
    }

    private void generateFileTree(String datasetId, FileTreeNode root) {
        String tree;
        try {
            tree = objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            throw new BusinessException(500, "数据集写入文件树失败,JSON序列化失败");
        }
        log.debug("数据集 {} 生成文件树: {}", datasetId, tree);
        this.lambdaUpdate().eq(DatasetInfo::getId, datasetId).set(DatasetInfo::getTree, tree).update();
    }

    private int countCompletedFiles(String datasetId) {
        return fileInfoService.lambdaQuery()
                .eq(FileInfo::getDatasetId, datasetId)
                .eq(FileInfo::getFileStatus, FileStatusEnum.SUCCESS)
                .count()
                .intValue();
    }

    private int countTotalFiles(FileTreeNode root) {
        return countFilesByTree(root);
    }

    private void updateDatasetStatus(String datasetId, int complete, int total, String status) {
        this.lambdaUpdate().eq(DatasetInfo::getId, datasetId)
                .set(DatasetInfo::getTotal, total)
                .set(DatasetInfo::getComplete, complete)
                .set(DatasetInfo::getDatasetStatus, status)
                .update();
    }

    private void updateDatasetErrorStatus(String datasetId) {
        this.lambdaUpdate()
                .eq(DatasetInfo::getId, datasetId)
                .set(DatasetInfo::getDatasetStatus, DatasetStatusEnum.ERROR)
                .update();

    }

    private int countFilesByTree(FileTreeNode root) {
        if (root == null) {
            return 0;
        }
        int count = 0;
        CopyOnWriteArrayList<FileTreeNode> children = root.getChildren();
        for (FileTreeNode child : children) {
            if (child.getType().equals(FileTypeEnum.FILE)) {
                count++;
            }
            count += countFilesByTree(child);
        }
        return count;
    }

    private boolean shouldTraverseFolder(File folder, int fileLevel) {
        return folder.exists() && folder.isDirectory() && fileLevel <= 9;
    }

    private void processFileOrFolder(String datasetId, String pathSuffix, File file, int fileLevel, FileTreeNode root) {
        if (file.isDirectory()) {
            processFolder(datasetId, pathSuffix, file, fileLevel, root);
        } else {
            processFile(datasetId, pathSuffix, file, fileLevel, root);
        }
    }

    private void processFolder(String datasetId, String pathSuffix, File folder, int fileLevel, FileTreeNode root) {
        String fileName = folder.getName();
        FileTreeNode curNode = new FileTreeNode(fileName, FileTypeEnum.DIR);
        root.getChildren().add(curNode);

        pathSuffix += "/" + fileName;
        traverseFolder(datasetId, pathSuffix, folder, fileLevel + 1, curNode);
    }

    private void processFile(String datasetId, String pathSuffix, File file, int fileLevel, FileTreeNode root) {
        String fileName = file.getName();
        String ext = FilenameUtils.getExtension(fileName).toLowerCase();

        if (shouldProcessFile(ext, fileName)) {
            FileTreeNode curNode = new FileTreeNode(fileName, FileTypeEnum.FILE);
            root.getChildren().add(curNode);
            uploadFileToS3ByFileType(datasetId, pathSuffix, file, fileLevel, curNode);
        }
    }

    private boolean shouldProcessFile(String ext, String fileName) {
        return !(validateExtension(ext) || fileName.startsWith("._"));
    }


    private void uploadFileToS3ByFileType(String datasetId, String pathSuffix, File file, Integer fileLevel, FileTreeNode root) {
        final String ext = FilenameUtils.getExtension(file.getName()).toLowerCase();
        String fileName = file.getName();
        FileInfo fileInfo = uploadToS3(datasetId, pathSuffix, fileName, file, fileLevel);
        root.setFileId(fileInfo.getId());
        handleFileType(ext, datasetId, pathSuffix, file, fileLevel, root, fileInfo);
    }

    private void handleFileType(String ext, String datasetId, String pathSuffix, File file, Integer fileLevel, FileTreeNode root, FileInfo fileInfo) {
        String fileId = fileInfo.getId();
        String filePath = fileInfo.getFilePath();
        String fileName = file.getName();
        switch (ext) {
            case PDF:
                parsePDFFile(fileId, datasetId, pathSuffix, file,filePath);
                break;
            case DOC:
            case DOCX:
                parseWORDFile(fileId, datasetId, fileName, file,filePath);
                break;
            case TXT:
            case JSON:
                parseTXTFile(fileId);
                break;
            case CSV:
                parseCSVFile(fileId, datasetId, fileName, file, filePath);
                break;
            case ZIP:
                handleZipFile(datasetId, pathSuffix, fileName, file, fileLevel, root);
                break;
            default:
                throw new BusinessException(500, "文件格式不支持");
        }
    }

    private void handleZipFile(String datasetId, String pathSuffix, String fileName, File file, Integer fileLevel, FileTreeNode root) {
        root.setType(FileTypeEnum.ZIP);
        unzipAndTraverseFolder(file, datasetId, fileName, root, pathSuffix, fileLevel);
    }

    private void parseTXTFile(String fileId) {
        FileInfo fileInfo = fileInfoService.getById(fileId);
        fileInfo.setParseFilePath(fileInfo.getFilePath());
        fileInfo.setFileStatus(FileStatusEnum.SUCCESS.toString());
        fileInfoService.updateById(fileInfo);
    }

    private void parsePDFFile(String fileId, String datasetId, String pathSuffix, File tmpFile, String filePath) {
        OCRFileParseRequest request = OCRFileParseRequest.builder()
                .datasetId(datasetId)
                .fileId(fileId)
                .pathSuffix(pathSuffix)
                //.filePath(tmpFile.getPath())
                .filePath(filePath)
                .build();
        try {
            String message = objectMapper.writeValueAsString(request);
            rabbitTemplate.convertAndSend(fileParseExchange, fileParseQueueRoutingKey, message);
            log.debug("数据集 {} 文件 {} 开始OCR解析,发送消息: {}", datasetId, fileId, message);
        } catch (JsonProcessingException e) {
            log.error("数据集 {} 解析PDF文件 {} 失败,原因: {}", datasetId, fileId, e.getMessage());
        }
    }

    private void parseCSVFile(String fileId, String datasetId, String fileName, File file,String filePath) {
        // TODO: parse csv file to md file
        FileParseRequest fileParseRequest = FileParseRequest.builder()
                .fileId(fileId)
                .datasetId(datasetId)
                .fileName(fileName)
                .filePath(filePath)
                .build();
        try {
            String message = objectMapper.writeValueAsString(fileParseRequest);
            rabbitTemplate.convertAndSend(fileParseExchange, "file.parse.csv", message);
            log.debug("数据集 {} 文件 {} 开始解析,发送消息: {}", datasetId, fileId, message);
        } catch (JsonProcessingException e) {
            log.error("数据集 {} 解析csv文件 {} 失败,原因: {}", datasetId, fileId, e.getMessage());
        }
    }

    private void parseWORDFile(String fileId, String datasetId, String fileName, File file,String filePath) {
        // TODO: parse word file to md file
        FileParseRequest fileParseRequest = FileParseRequest.builder()
                .fileId(fileId)
                .datasetId(datasetId)
                .fileName(fileName)
                .filePath(filePath)
                .build();
        try {
            String message = objectMapper.writeValueAsString(fileParseRequest);
            rabbitTemplate.convertAndSend(fileParseExchange, "file.parse.word", message);
            log.debug("数据集 {} 文件 {} 开始解析,发送消息: {}", datasetId, fileId, message);
        } catch (JsonProcessingException e) {
            log.error("数据集 {} 解析Word文件 {} 失败,原因: {}", datasetId, fileId, e.getMessage());
        }
    }


    private FileInfo uploadToS3(String datasetId, String pathSuffix, String fileName, File tmpFile, Integer fileLevel) {
        log.debug("数据集 {} 文件 {} 正在上传至s3", datasetId, fileName);

        String s3Path = s3Service.uploadObject(datasetId, pathSuffix, tmpFile, fileName);

        LocalDateTime time = new Timestamp(System.currentTimeMillis()).toLocalDateTime();
        FileInfo fileInfo = FileInfo.builder()
                .datasetId(datasetId)
                .fileLevel(fileLevel)
                .filePath(s3Path)
                .parseFilePath("")
                .fileName(fileName)
                .fileType(FilenameUtils.getExtension(fileName).toLowerCase())
                .fileSize(tmpFile.length() / 1024)
                .fileStatus(DatasetStatusEnum.PARSING.toString())
                .createTime(time)
                .updateTime(time)
                .build();

        fileInfoService.saveFileInfo(fileInfo, 3);
        log.debug("数据集 {} 文件 {} 成功上传至s3,路径为 {}", datasetId, fileName, s3Path);
        return fileInfo;
    }

    @Override
    public DatasetListResponse getDatasetInfoList(Integer page, Integer size,
                                                  String sortAttribute, String sortDirection,
                                                  String datasetName, String tag, Long startTime, Long endTime,
                                                  String userId) {
        QueryWrapper<DatasetInfo> queryWrapper = buildQueryWrapper(datasetName, tag, startTime, endTime,
                sortAttribute, sortDirection, userId);

        // 返回查询结果总数
        long totalCount = this.count(queryWrapper);

        // 根据page和size进行分页
        List<DatasetInfo> datasetInfoList = this.page(new Page<>(page, size), queryWrapper).getRecords();

        return generateDatasetQueryResponse(datasetInfoList, totalCount);
    }

    private QueryWrapper<DatasetInfo> buildQueryWrapper(String datasetName, String tag, Long startTime, Long endTime,
                                                        String sortAttribute, String sortDirection, String userId) {
        QueryWrapper<DatasetInfo> queryWrapper = new QueryWrapper<>();

        queryWrapper.lambda().eq(DatasetInfo::getUploaderId, userId);
        tag = handleTag(tag);
        handleDataSetNameAndTag(datasetName, tag, queryWrapper);
        handleTimeRange(startTime, endTime, queryWrapper);
        handleSorting(sortAttribute, sortDirection, queryWrapper);

        return queryWrapper;
    }

    private String handleTag(String tag) {
        if (StringUtils.isNotBlank(tag)) {
            tag = tag.replaceAll("[\\[\\]\",]", "");
        }
        return tag;
    }

    private void handleDataSetNameAndTag(String datasetName, String tag, QueryWrapper<DatasetInfo> queryWrapper) {
        if (StringUtils.isNotBlank(datasetName)) {
            queryWrapper.lambda().like(DatasetInfo::getDatasetName, datasetName);
        } else if (StringUtils.isNotBlank(tag)) {
            queryWrapper.lambda().like(DatasetInfo::getTags, tag);
        }
    }

    private void handleTimeRange(Long startTime, Long endTime, QueryWrapper<DatasetInfo> queryWrapper) {
        if (startTime != null) {
            queryWrapper.lambda().ge(DatasetInfo::getCreateTime, startTime);
        }
        if (endTime != null) {
            queryWrapper.lambda().le(DatasetInfo::getCreateTime, endTime);
        }
    }

    private void handleSorting(String sortAttribute, String sortDirection, QueryWrapper<DatasetInfo> queryWrapper) {
        if (StringUtils.isNotBlank(sortAttribute) && StringUtils.isNotBlank(sortDirection)) {
            if ("asc".equalsIgnoreCase(sortDirection)) {
                queryWrapper.orderByAsc(StringUtils.camelToUnderline(sortAttribute));
            } else if ("desc".equalsIgnoreCase(sortDirection)) {
                queryWrapper.orderByDesc(StringUtils.camelToUnderline(sortAttribute));
            }
        }
    }

    private DatasetListResponse generateDatasetQueryResponse(List<DatasetInfo> datasetInfoList, Long totalCount) {
        ArrayList<DatasetListResponse.DatasetInfoVO> datasetInfoVOList = new ArrayList<>();
        datasetInfoList.forEach(datasetInfo -> {
            String datasetId = datasetInfo.getId();
            List<DatasetListResponse.QATaskInfo> relatedTask = qaTaskDatasetsService.getRelatedQATaskByDatasetId(datasetId);

            DatasetListResponse.DatasetInfoVO datasetInfoVO;
            try {
                datasetInfoVO = DatasetListResponse.DatasetInfoVO.builder()
                        .id(datasetInfo.getId())
                        .datasetName(datasetInfo.getDatasetName())
                        .datasetSize(datasetInfo.getDatasetSize())
                        .datasetStatus(datasetInfo.getDatasetStatus())
                        .uploaderId(datasetInfo.getUploaderId())
                        .total(datasetInfo.getTotal())
                        .complete(datasetInfo.getComplete())
                        .tags(objectMapper.readValue(datasetInfo.getTags(), new TypeReference<List<String>>() {
                        }))
                        .createTime(datasetInfo.getCreateTime())
                        .updateTime(datasetInfo.getUpdateTime())
                        .relatedQATaskList(relatedTask)
                        .build();
            } catch (JsonProcessingException e) {
                throw new BusinessException(500, "JSON字符串转储对象失败");
            }
            BeanUtils.copyProperties(datasetInfo, datasetInfoVO);

            datasetInfoVOList.add(datasetInfoVO);
        });
        return DatasetListResponse.builder()
                .datasetInfoVOList(datasetInfoVOList)
                .total(totalCount).build();
    }

    @Override
    public void renameDataset(String datasetId, String datasetName) {
        this.lambdaUpdate().eq(DatasetInfo::getId, datasetId)
                .set(DatasetInfo::getDatasetName, datasetName)
                .update();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDatasets(List<String> deleteIds) {
        Long count = qaTaskDatasetsService.lambdaQuery()
                .in(QATaskDatasets::getDatasetId, deleteIds).count();
        if (count > 0) {
            throw new BusinessException(500, "数据集有关联任务,不能删除");
        }
        deleteIds.forEach(deleteId -> s3Service.deleteDirectory(deleteId));
        baseMapper.deleteBatchIds(deleteIds);
    }

    @Override
    public void changeTags(String datasetId, List<String> tags) {
        try {
            this.lambdaUpdate().eq(DatasetInfo::getId, datasetId)
                    .set(DatasetInfo::getTags, objectMapper.writeValueAsString(tags))
                    .update();
        } catch (JsonProcessingException e) {
            throw new BusinessException(500, "标签转储JSON字符串失败");
        }
    }

    @Override
    public FilePreviewResponse getFilePath(String fileId) {
        FileInfo fileInfo = fileInfoService.lambdaQuery().eq(FileInfo::getId, fileId).one();
        return FilePreviewResponse.builder()
                .fileId(fileId)
                .fileType(fileInfo.getFileType())
                .fileName(fileInfo.getFileName())
                .srcFilePath(fileInfo.getFilePath())
                .parseFilePath(fileInfo.getParseFilePath())
                .build();
    }

    @Override
    public FileTreeNode getTree(String datasetId) {
        DatasetInfo datasetInfo = this.getById(datasetId);
        try {
            return objectMapper.readValue(datasetInfo.getTree(), FileTreeNode.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException(500, "文件树JSON字符串转储对象失败");
        }
    }

}
