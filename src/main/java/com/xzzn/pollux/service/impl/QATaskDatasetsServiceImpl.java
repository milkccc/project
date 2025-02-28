package com.xzzn.pollux.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xzzn.pollux.entity.QATask;
import com.xzzn.pollux.entity.QATaskDatasets;
import com.xzzn.pollux.mapper.QATaskDatasetsMapper;
import com.xzzn.pollux.mapper.QATaskMapper;
import com.xzzn.pollux.model.vo.response.dataset.DatasetListResponse;
import com.xzzn.pollux.service.IQATaskDatasetsService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 任务数据集关联表 服务实现类
 * </p>
 *
 * @author xzzn
 */
@Service
public class QATaskDatasetsServiceImpl extends ServiceImpl<QATaskDatasetsMapper, QATaskDatasets> implements IQATaskDatasetsService {

    @Resource
    private QATaskMapper qaTaskMapper;

    public List<DatasetListResponse.QATaskInfo> getRelatedQATaskByDatasetId(String datasetId) {
        List<DatasetListResponse.QATaskInfo> qaTaskInfoList = new ArrayList<>();
        List<String> qaTaskIdList = this.lambdaQuery()
                .eq(QATaskDatasets::getDatasetId, datasetId)
                .list().stream().map(QATaskDatasets::getTaskId).collect(Collectors.toList());

        if (!qaTaskIdList.isEmpty()) {
            List<QATask> qaTasks = qaTaskMapper.selectList(new QueryWrapper<QATask>().in("id", qaTaskIdList));
            qaTasks.forEach(qaTask -> qaTaskInfoList.add(new DatasetListResponse.QATaskInfo(qaTask.getId(), qaTask.getTaskName())));
        }

        return qaTaskInfoList;
    }
}
