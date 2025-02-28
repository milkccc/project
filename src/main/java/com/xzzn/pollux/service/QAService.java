package com.xzzn.pollux.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xzzn.pollux.common.enums.DatasetStatusEnum;
import com.xzzn.pollux.common.exception.BusinessException;
import com.xzzn.pollux.entity.DatasetInfo;
import com.xzzn.pollux.entity.QATask;
import com.xzzn.pollux.entity.QATaskLocal;
import com.xzzn.pollux.entity.es.QADocument;
import com.xzzn.pollux.mapper.QATaskFilesMapper;
import com.xzzn.pollux.mapper.QATaskLocalMapper;
import com.xzzn.pollux.mapper.QATaskMapper;
import com.xzzn.pollux.model.pojo.QAPair;
import com.xzzn.pollux.model.vo.request.qa.*;
import com.xzzn.pollux.service.impl.QATaskFilesServiceImpl;
import com.xzzn.pollux.service.impl.QATaskServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * QA服务类
 *
 * @author xzzn
 */
@Slf4j
@Service
public class QAService {

//    @Resource
//    private QATaskFilesServiceImpl qaTaskFilesService;

    @Resource
    private QATaskServiceImpl qaTaskService;

    @Resource
    private ESService esService;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private QATaskFilesMapper qaTaskFilesMapper;

    @Resource
    private QATaskMapper qaTaskMapper;


    /**
     * 获取QA对转JSON字符串
     *
     * @param qaExportRequest QA导出请求
     * @return 返回JSON格式的QA
     */
    @Transactional
    public String exportQA(QAExportRequest qaExportRequest) {
        log.debug("开始导出QA");

        boolean isAll = qaExportRequest.isAll();
        List<QAExportRequest.QAExportInfo> qaExportInfoList = qaExportRequest.getQaExportInfoList();
        List<String> fileIdList = qaExportRequest.getFileIdList();
        List<QAExportRequest.ExcludeInfo> excludeInfoList = qaExportRequest.getExcludeInfoList();
        String taskId = qaExportRequest.getTaskId();

        existsTask(taskId);
        List<QAPair> qaPairs = getQAPairs(isAll, qaExportInfoList, fileIdList, excludeInfoList, taskId);

        try {
            return objectMapper.writeValueAsString(qaPairs);
        } catch (JsonProcessingException e) {
            throw new BusinessException(500, "QA对转储JSON字符串失败");
        }
    }

//    /**
//     * 导入本地QA对
//     *
//     * @param importQAs QA文件
//     * @param taskId
//     * @return 返回JSON格式的QA
//     */
//    @Transactional(rollbackFor = Exception.class)
//    public void importQA(String taskId,MultipartFile importQAs)  {
//        existsTask(taskId);
//        int totalqacount = esService.saveQAToESfromlocal(taskId, importQAs);
//        saveRecord(taskId,totalqacount);
//    }


    /**
     * 删除QA对
     *
     * @param qaDeleteRequest QA删除请求
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteQA(QADeleteRequest qaDeleteRequest) {
        try {
            log.debug("开始删除QA:");

            int qaDeleteCount = 0;
            int reviewDeleteCount = 0;

            List<QADeleteRequest.QADeleteInfo> qaDeleteInfoList = qaDeleteRequest.getQaDeleteInfoList();
            List<String> fileIdList = qaDeleteRequest.getFileIdList();
            List<QADeleteRequest.ExcludeInfo> excludeInfoList = qaDeleteRequest.getExcludeInfoList();
            String taskId = qaDeleteRequest.getTaskId();

            existsTask(taskId);

            int[] resCount;
            resCount = deleteQAPairsInQADeleteInfoList(qaDeleteInfoList, taskId);
            qaDeleteCount += resCount[0];
            reviewDeleteCount += resCount[1];

            resCount = deleteQAPairsInFileIdList(fileIdList, taskId);
            qaDeleteCount += resCount[0];
            reviewDeleteCount += resCount[1];

            resCount = deleteQAPairsInExcludeInfoList(excludeInfoList, taskId);
            qaDeleteCount += resCount[0];
            reviewDeleteCount += resCount[1];

            qaTaskMapper.updateQACount(taskId, -qaDeleteCount);
            qaTaskMapper.updateReviewCount(taskId, -reviewDeleteCount);
            log.debug("删除QA结束");
        } catch (Exception e) {
            throw new BusinessException(500, "删除QA对失败");
        }
    }

    /**
     * 修改QA对
     *
     * @param qaUpdateRequest 修改QA对请求
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateQA(QAUpdateRequest qaUpdateRequest) {
        String taskId = qaUpdateRequest.getTaskId();

        QADocument oldQA = esService.getQADocumentById(taskId, qaUpdateRequest.getId());
        if (oldQA == null) {
            throw new BusinessException(500, "QA对不存在");
        }
        // 判断该QA是否被修改过
        if (oldQA.getIsModify() == null || oldQA.getIsModify().isEmpty() || oldQA.getIsModify().equals("false")) {
            // TODO:  该QA未被修改过，需要将原始QA保存到QA对集合中
            oldQA.setIsModify("true");
            QADocument qaDocument = esService.saveQADocument(oldQA, taskId);
            String sourceId = qaDocument.getId();
            esService.updateQADocument(taskId, qaUpdateRequest.getId(), qaUpdateRequest.getQuestion(), qaUpdateRequest.getAnswer(), sourceId);
        }
        esService.updateQADocument(taskId, qaUpdateRequest.getId(), qaUpdateRequest.getQuestion(), qaUpdateRequest.getAnswer(), oldQA.getIsModify());
    }

    public List<QAPair> qaDocumentsToQAPairs(List<QADocument> qaDocuments) {
        List<QAPair> qaPairs = new ArrayList<>();
        for (QADocument qaDocument : qaDocuments) {
            qaPairs.add(new QAPair(qaDocument.getQuestion(), qaDocument.getAnswer()));
        }
        return qaPairs;
    }

    private void existsTask(String taskId) {
        QATask qaTask = qaTaskService.getById(taskId);
        if (qaTask == null) {
            throw new BusinessException(500, "任务" + taskId + "不存在");
        }
    }

    public List<QAPair> getQAPairs(boolean isAll, List<QAExportRequest.QAExportInfo> qaExportInfoList,
                                    List<String> fileIdList, List<QAExportRequest.ExcludeInfo> excludeInfoList, String taskId) {
        List<QAPair> qaPairs = new ArrayList<>();
        if (isAll) {
            log.debug("导出全部QA");
            List<QADocument> qaDocuments = esService.getAllQADocument(taskId);
            qaPairs.addAll(qaDocumentsToQAPairs(qaDocuments));
        } else {
            log.debug("导出部分QA:");
            getQAPairsByQAExportInfoList(qaPairs, qaExportInfoList, taskId);
            getQAPairsByFileIdList(qaPairs, fileIdList, taskId);
            getQAPairsByExcludeInfoList(qaPairs, excludeInfoList, taskId);
        }
        return qaPairs;
    }

    private void getQAPairsByQAExportInfoList(List<QAPair> qaPairs, List<QAExportRequest.QAExportInfo> qaExportInfoList, String taskId) {
        log.debug("1.导出单选的QA列表");
        if (qaExportInfoList != null) {
            for (QAExportRequest.QAExportInfo qaExportInfo : qaExportInfoList) {
                List<String> ids = qaExportInfo.getQaIds();
                List<QADocument> qaDocuments = esService.getQADocumentByIds(taskId, ids);
                qaPairs.addAll(qaDocumentsToQAPairs(qaDocuments));
            }
        }
    }

    private void getQAPairsByFileIdList(List<QAPair> qaPairs, List<String> datasetIdList, String taskId) {
        log.debug("2.导出文件的QA列表");
        if (datasetIdList != null) {
            for (String fileId : datasetIdList) {
                List<QADocument> qaDocuments = esService.getQAByFile(taskId, fileId);
                qaPairs.addAll(qaDocumentsToQAPairs(qaDocuments));
            }
        }
    }

    private void getQAPairsByExcludeInfoList(List<QAPair> qaPairs, List<QAExportRequest.ExcludeInfo> excludeInfoList, String taskId) {
        log.debug("3.导出选中文档但是排除某些QA的文件列表");
        if (excludeInfoList != null) {
            for (QAExportRequest.ExcludeInfo excludeInfo : excludeInfoList) {
                String fileId = excludeInfo.getFileId();
                List<String> ids = excludeInfo.getIds();
                List<QADocument> qaDocuments = esService.getQAByFileExcludeInfoList(taskId, fileId, ids);
                qaPairs.addAll(qaDocumentsToQAPairs(qaDocuments));
            }
        }
    }

    private int[] deleteQAPairsInQADeleteInfoList(List<QADeleteRequest.QADeleteInfo> qaDeleteInfoList, String taskId) {
        int[] resCount = new int[2];
        log.debug("1.删除单选的QA列表");
        if (qaDeleteInfoList != null) {
            for (QADeleteRequest.QADeleteInfo qaDeleteInfo : qaDeleteInfoList) {
                List<String> ids = qaDeleteInfo.getIds();
                int[] counts = esService.deleteByIds(taskId, ids);
                resCount[0] += counts[0];
                resCount[1] += counts[1];
                qaTaskFilesMapper.updateQACount(taskId, qaDeleteInfo.getFileId(), -counts[0]);
            }
        }
        return resCount;
    }

    private int[] deleteQAPairsInFileIdList(List<String> fileIdList, String taskId) {
        int[] resCount = new int[2];
        log.debug("2.删除文件的QA列表");
        if (fileIdList != null) {
            for (String fileId : fileIdList) {
                int[] counts = esService.deleteByFileId(taskId, fileId);
                resCount[0] += counts[0];
                resCount[1] += counts[1];
                qaTaskFilesMapper.updateQACount(taskId, fileId, -counts[0]);
            }
        }
        return resCount;
    }

    private int[] deleteQAPairsInExcludeInfoList(List<QADeleteRequest.ExcludeInfo> excludeInfoList, String taskId) {
        int[] resCount = new int[2];
        log.debug("3.删除选中文档但是排除某些QA的文件列表");
        if (excludeInfoList != null) {
            for (QADeleteRequest.ExcludeInfo excludeInfo : excludeInfoList) {
                String fileId = excludeInfo.getFileId();
                int[] counts = esService.deleteByFileIdExcludeIdList(taskId, fileId, excludeInfo.getIds());
                resCount[0] += counts[0];
                resCount[1] += counts[1];
                qaTaskFilesMapper.updateQACount(taskId, fileId, -counts[0]);
            }
        }
        return resCount;
    }


}
