package com.xzzn.pollux.service;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xzzn.pollux.common.exception.BusinessException;
import com.xzzn.pollux.entity.QATask;
import com.xzzn.pollux.entity.QATaskFiles;
import com.xzzn.pollux.entity.ReviewConfig;
import com.xzzn.pollux.entity.TaskReview;
import com.xzzn.pollux.entity.es.QADocument;
import com.xzzn.pollux.mapper.QATaskMapper;
import com.xzzn.pollux.mapper.TaskReviewMapper;
import com.xzzn.pollux.model.dto.UserReviewProgressDTO;
import com.xzzn.pollux.model.vo.response.task.QAUnallocatedFileIdResponse;
import com.xzzn.pollux.service.impl.QATaskFilesServiceImpl;
import com.xzzn.pollux.service.impl.ReviewConfigServiceImpl;
import com.xzzn.pollux.service.impl.TaskReviewServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 审核服务类
 *
 * @author xzzn
 */
@Slf4j
@Service
public class ReviewService {

    @Resource
    private ESService esService;

    @Resource
    private TaskReviewServiceImpl taskReviewService;

    @Resource
    private QATaskFilesServiceImpl qaTaskFilesService;

    @Resource
    private ReviewConfigServiceImpl reviewConfigService;

    @Resource
    private QATaskMapper qaTaskMapper;

    @Resource
    private TaskReviewMapper taskReviewMapper;

    @Transactional(rollbackFor = Exception.class)
    public void normalReview(String userId, String taskId, String qaId) {
        hasQATaskPermission(taskId, userId);
        hasReviewPermission(taskId, qaId, userId);
        QATask qaTask = hasQATask(taskId);
        ReviewConfig reviewConfig = hasReviewConfig(qaTask);

        if (esService.updateQAReviewStatus(taskId, qaId) && !reviewConfigService.hasTwoStep(taskId)) {
            qaTaskMapper.updateReviewCount(taskId, 1);
        }
        if (Boolean.FALSE.equals(reviewConfig.getIsStepTwo())) {
            taskReviewMapper.updateReviewCount(taskId, userId, 1);
        }

    }

    @Transactional(rollbackFor = Exception.class)
    public void scoreReview(String userId, String taskId, String qaId, String score) {
        hasQATaskPermission(taskId, userId);
        hasReviewPermission(taskId, qaId, userId);
        QATask qaTask = hasQATask(taskId);
        hasReviewConfig(qaTask);

        if (esService.updateQAReviewStatusAndScore(taskId, qaId, score)) {
            qaTaskMapper.updateReviewCount(taskId, 1);

        }
        taskReviewMapper.updateReviewCount(taskId, userId, 1);
    }

    /**
     * 获取审核进度
     *
     * @param taskId 任务id
     * @param userId 用户id
     * @return 审核进度
     */
    public UserReviewProgressDTO getReviewProgress(String taskId, String userId, String operateUserId) {
        hasQATaskPermission(taskId, operateUserId);
        TaskReview taskReview = getTaskReview(taskId, userId);

        int curAllocate = taskReview.getAllocatedQANum();
        Integer totalReview = taskReview.getReviewTotalNum();
        int curReview = taskReview.getReviewCurNum();
        long total = esService.getQACountByTaskId(taskId);

        return UserReviewProgressDTO.builder()
                .total(total)
                .totalReviewed(totalReview)
                .curAllocated(curAllocate)
                .curReviewed(curReview)
                .totalProgress(getReviewTotalProgress(total, totalReview))
                .curProgress(getReviewCurProgress(curAllocate, curReview))
                .build();
    }

    public List<String> getAllocatedQAIds(String taskId, String userId, String operateUserId) {
        hasQATaskPermission(taskId, operateUserId);
        TaskReview taskReview = getTaskReview(taskId, userId);
        // 打印获取到的 taskReview
        log.info("TaskReview: {}", taskReview);

        if (taskReview.getAllocatedQAList() == null || taskReview.getAllocatedQAList().equals("")) {
            return new ArrayList<>();
        }
        ObjectMapper objectMapper = new ObjectMapper();
        ArrayList<String> allocatedQAList;
        try {
            allocatedQAList = objectMapper.readValue(taskReview.getAllocatedQAList(), new TypeReference<ArrayList<String>>() {
            });
        } catch (JsonProcessingException e) {
            log.error("json转换异常");
            throw new BusinessException(500, "json转换异常");
        }
        // 打印解析后的 allocatedQAList
        log.info("Allocated QA List: {}", allocatedQAList);

        return allocatedQAList;
    }

    /**
     * 分配QA
     *
     * @param taskId 任务id
     * @param userId 用户id
     */
    @Transactional(rollbackFor = Exception.class)
    public void allocateQA(String taskId, String userId) {

        TaskReview taskReview = isLegalAllocate(taskId, userId);
        // 分配任务
        String lastChildFileId = taskReview.getLastChildFileId();
        List<QADocument> qaDocumentList = new ArrayList<>();
        if (lastChildFileId != null && !lastChildFileId.equals("")) {
            // 尝试获取100个未分配的QA对
            List<QADocument> qaDocuments = allocateQAByFileId(taskId, lastChildFileId, userId, 100);
            qaDocumentList.addAll(qaDocuments);
        }
        // 判断是否找到了100个未分配的QA对,如果没有,则从其他childFile中找,并更新lastChildFileId
        if (qaDocumentList.size() < 100) {
            int leftNum = 100 - qaDocumentList.size();
            List<String> fileIdList = qaTaskFilesService.lambdaQuery()
                    .eq(QATaskFiles::getTaskId, taskId)
                    .list()
                    .stream().map(QATaskFiles::getFileId).collect(Collectors.toList());
            for (String fileId : fileIdList) {
                if (!fileId.equals(lastChildFileId)) {
                    List<QADocument> qaDocuments = allocateQAByFileId(taskId, fileId, userId, leftNum);
                    qaDocumentList.addAll(qaDocuments);
                    // 更新lastChildFileId
                    taskReview.setLastChildFileId(fileId);
                    leftNum = 100 - qaDocumentList.size();
                    if (leftNum <= 0) {
                        break;
                    }
                }
            }
        }
        // 更新QA对
        taskReview.setAllocatedQAList(JSON.toJSONString(qaDocumentList.stream().map(QADocument::getId).collect(Collectors.toList())));
        taskReview.setAllocatedQANum(qaDocumentList.size());
        taskReview.setReviewCurNum(0);
        taskReviewService.updateById(taskReview);
    }
    /**
     * 平均分配QA
     *
     * @param taskId 任务id
     * @param userIds 用户id
     */
    @Transactional(isolation = Isolation.SERIALIZABLE, rollbackFor = Exception.class)
    public void equalAllocate(String taskId, List<String> userIds) throws InterruptedException {
        synchronized (this) {
            if (!reviewConfigService.isSetReviewConfig(taskId)) {
                throw new BusinessException(500, "未设置审核配置！");
            }
            int userNum = userIds.size();
            int unallocatedQANum = countUnallocatedQAByFileIds(taskId).getTotalUnallocatedCount();
            int oneNum = unallocatedQANum / userNum;
            int restNum = unallocatedQANum % userNum;

            for (int i = 0; i < userNum; i++) {
                int numToAllocate = (i == userNum - 1) ? oneNum + restNum : oneNum;
                equalAllocateQA(taskId, userIds.get(i), numToAllocate);
                Thread.sleep(1000);
            }
        }
    }

    private int equalAllocateQA(String taskId, String userId, int numToAllocate) {
        TaskReview taskReview = isLegalAllocate(taskId, userId);
        String lastChildFileId = taskReview.getLastChildFileId();
        List<QADocument> qaDocumentList = new ArrayList<>();

        if (lastChildFileId != null && !lastChildFileId.isEmpty()) {
            List<QADocument> qaDocuments = allocateQAByFileId(taskId, lastChildFileId, userId, numToAllocate);
            qaDocumentList.addAll(qaDocuments);

        }

        if (qaDocumentList.size() < numToAllocate) {
            int leftNum = numToAllocate - qaDocumentList.size();
            List<String> fileIdList = qaTaskFilesService.lambdaQuery()
                    .eq(QATaskFiles::getTaskId, taskId)
                    .list()
                    .stream()
                    .map(QATaskFiles::getFileId)
                    .collect(Collectors.toList());

            for (String fileId : fileIdList) {
                if (!fileId.equals(lastChildFileId)) {
                    List<QADocument> qaDocuments = allocateQAByFileId(taskId, fileId, userId, leftNum);
                    qaDocumentList.addAll(qaDocuments);
                    taskReview.setLastChildFileId(fileId);
                    leftNum = numToAllocate - qaDocumentList.size();
                    if (leftNum <= 0) {
                        break;
                    }
                }
            }
        }
        log.info("用户：{}，分到的QA对有:{}个", userId, qaDocumentList.size());
        taskReview.setAllocatedQAList(JSON.toJSONString(
                qaDocumentList.stream().map(QADocument::getId).collect(Collectors.toList())));
        taskReview.setAllocatedQANum(qaDocumentList.size());
        taskReview.setReviewCurNum(0);
        taskReviewService.updateById(taskReview);
        return qaDocumentList.size();
    }


    public QAUnallocatedFileIdResponse countUnallocatedQAByFileIds(String taskId) {
        // 获取指定 taskId 下的所有文件 ID 列表
        List<String> fileIdList = qaTaskFilesService.lambdaQuery()
                .eq(QATaskFiles::getTaskId, taskId)
                .list()
                .stream()
                .map(QATaskFiles::getFileId)
                .collect(Collectors.toList());

        // 存放每个文件 ID 和对应的未分配 QA 对数量的 Chunk 列表
        List<QAUnallocatedFileIdResponse.Chunk> chunkList = new ArrayList<>();
        int totalUnallocatedCount = 0;

        for (String fileId : fileIdList) {
            // 获取当前 fileId 对应的未被分配的 QA 对数量
            int unallocatedCount = esService.getUnallocatedQAByFileCount(taskId, fileId,false);
            log.info("文件{}未分配的数量是:{}", fileId, unallocatedCount);
            int allocatedCount = esService.getUnallocatedQAByFileCount(taskId, fileId,true);
            // 创建 Chunk 对象并加入列表
            chunkList.add(QAUnallocatedFileIdResponse.Chunk.builder()
                    .fileId(fileId)
                    .UnallocatedCount(unallocatedCount)
                    .totalCount(unallocatedCount+allocatedCount)
                    .build());

            totalUnallocatedCount += unallocatedCount;
        }

        log.info("所有文件未分配的总数量: {}", totalUnallocatedCount);

        // 返回 QAUnallocatedFileIdResponse 对象
        return QAUnallocatedFileIdResponse.builder()
                .chunkList(chunkList)
                .totalUnallocatedCount(totalUnallocatedCount)
                .build();
    }

    /**
     * 某个用户对于某个任务来说，是否满足分配条件
     *
     * @param taskId 任务id
     * @param userId 用户id
     * @param fileId 文件id
     * @param numToAllocate 分配数量
     */
    @Transactional(rollbackFor = Exception.class)
    public void allocateQAsToUsers(String taskId, String userId, String fileId, int numToAllocate) {
        TaskReview taskReview = isLegalAllocate(taskId, userId);
        int unallocatedCount = esService.getUnallocatedQAByFileCount(taskId, fileId,false);
        // 分配任务
        List<QADocument> qaDocumentList = new ArrayList<>();
        if(unallocatedCount > 0 && unallocatedCount >= numToAllocate) {
            List<QADocument> qaDocuments = allocateQAByFileId(taskId, fileId, userId, numToAllocate);
            qaDocumentList.addAll(qaDocuments);

            // 更新QA对
            taskReview.setAllocatedQAList(JSON.toJSONString(qaDocumentList.stream().map(QADocument::getId).collect(Collectors.toList())));
            taskReview.setAllocatedQANum(qaDocumentList.size());
            taskReview.setReviewCurNum(0);
            taskReviewService.updateById(taskReview);
        }
    }
    /**
     * 某个用户对于某个任务来说，是否满足分配条件
     *
     * @param taskId 任务id
     * @param userId 用户id
     */
    public TaskReview isLegalAllocate(String taskId, String userId) {
        hasQATaskPermission(taskId, userId);
        TaskReview taskReview = getTaskReview(taskId, userId);

        if (!Objects.equals(taskReview.getAllocatedQANum(), taskReview.getReviewCurNum())) {
            throw new BusinessException(200, "该用户尚未审核完已分配的任务");
        }
        return taskReview;
    }

    public List<QADocument> allocateQAByFileId(String taskId, String fileId, String userId, int leftNum) {
        synchronized (fileId.intern()) {
            // 从ES中获取当前fileId对应的未被分配的QA
            List<QADocument> qaDocumentList = esService.getUnallocatedQAByFile(taskId, fileId, leftNum);

            // 更新es中这些qa对的is_allocated为true
            if (qaDocumentList.isEmpty()) {
                return qaDocumentList;
            }
            qaDocumentList.forEach(qaDocument -> {
                qaDocument.setAllocated(true);
                qaDocument.setAllocatedUserId(userId);
            });
            Document document = Document.create();
            document.putIfAbsent("is_allocated", true); //更新后的内容
            document.putIfAbsent("allocated_user_id", userId); //更新后的内容
            esService.updateQAIsAllocated(taskId, qaDocumentList, document);
            return qaDocumentList;
        }
    }

    /**
     * 取消分配QA
     *
     * @param taskId 任务id
     * @param userId 用户id
     */
    @Transactional(rollbackFor = Exception.class)
    public void deallocateQA(String taskId, String userId, String operateUserId) {
        List<String> allocatedQAIds = this.getAllocatedQAIds(taskId, userId, operateUserId);
        List<QADocument> qaDocuments = esService.getQADocumentByIds(taskId, allocatedQAIds).stream()
                .filter(qaDocument -> !qaDocument.isReview())
                .collect(Collectors.toList());
        qaDocuments.forEach(qaDocument -> {
            qaDocument.setAllocated(false);
            qaDocument.setAllocatedUserId(null);
        });
        Document document = Document.create();
        document.putIfAbsent("is_allocated", false); //更新后的内容
        document.putIfAbsent("allocated_user_id", null); //更新后的内容
        esService.updateQAIsAllocated(taskId, qaDocuments, document);
        taskReviewService.deallocateTaskReview(taskId, userId);
    }

    private boolean hasQATaskPermission(String taskId, String userId) {
        TaskReview taskReview = taskReviewService.lambdaQuery()
                .eq(TaskReview::getTaskId, taskId)
                .eq(TaskReview::getUserId, userId)
                .one();
        if (taskReview == null) {
            throw new BusinessException(500, "该用户无此任务的可见权限");
        }
        return true;
    }

    private TaskReview getTaskReview(String taskId, String userId) {
        TaskReview taskReview = taskReviewService.lambdaQuery()
                .eq(TaskReview::getTaskId, taskId)
                .eq(TaskReview::getUserId, userId)
                .one();
        if (taskReview == null) {
            throw new BusinessException(500, "该用户无此任务的可见权限");
        }
        return taskReview;
    }

    public void hasReviewPermission(String taskId, String qaId, String userId) {
        List<String> allocatedQAList = getAllocatedQAIds(taskId, userId, userId);
        if (!allocatedQAList.contains(qaId)) {
            throw new BusinessException(500, "该用户无权限审核此条QA");
        }
    }

    private QATask hasQATask(String taskId) {
        QATask qaTask = qaTaskMapper.selectById(taskId);
        if (qaTask == null) {
            throw new BusinessException(500, "任务不存在");
        }
        return qaTask;
    }

    private ReviewConfig hasReviewConfig(QATask qaTask) {
        if (qaTask.getReviewConfigId() == null) {
            throw new BusinessException(403, "任务未设置审核配置");
        }
        return reviewConfigService.getById(qaTask.getReviewConfigId());
    }

    private double getReviewCurProgress(long curAllocate, int curReview) {
        return curAllocate == 0 ? 0 : ((double) curReview) / curAllocate;
    }

    private double getReviewTotalProgress(long total, Integer totalReview) {
        return total == 0 ? 0 : ((double) totalReview) / total;
    }
}
