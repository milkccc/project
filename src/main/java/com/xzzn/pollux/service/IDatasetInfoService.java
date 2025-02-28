package com.xzzn.pollux.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xzzn.pollux.entity.DatasetInfo;
import com.xzzn.pollux.model.pojo.FileTreeNode;
import com.xzzn.pollux.model.vo.response.dataset.DatasetListResponse;
import com.xzzn.pollux.model.vo.response.dataset.FilePreviewResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * <p>
 * 数据集信息表 服务类
 * </p>
 *
 * @author xzzn
 */
public interface IDatasetInfoService extends IService<DatasetInfo> {

    String generateDataSet(String userId, MultipartFile datasetFile, List<String> tags);

    DatasetListResponse getDatasetInfoList(Integer page, Integer size,
                                           String sortAttribute, String sortDirection,
                                           String datasetName, String tag, Long startTime, Long endTime,
                                           String userId);

    void renameDataset(String datasetId, String datasetName);

    void deleteDatasets(List<String> deleteIds);

    void changeTags(String datasetId, List<String> tags);

    FilePreviewResponse getFilePath(String fileId);

    FileTreeNode getTree(String datasetId);
}
