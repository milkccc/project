package com.xzzn.pollux.controller;

import com.xzzn.pollux.common.BaseResponse;
import com.xzzn.pollux.common.DeleteRequest;
import com.xzzn.pollux.common.ListResponse;
import com.xzzn.pollux.common.ResultResponse;
import com.xzzn.pollux.entity.User;
import com.xzzn.pollux.model.pojo.FileTreeNode;
import com.xzzn.pollux.model.vo.request.dataset.DatasetChangeTagsRequest;
import com.xzzn.pollux.model.vo.request.dataset.DatasetImportRequest;
import com.xzzn.pollux.model.vo.response.dataset.DatasetListResponse;
import com.xzzn.pollux.model.vo.response.dataset.FilePreviewResponse;
import com.xzzn.pollux.service.impl.DatasetInfoServiceImpl;
import com.xzzn.pollux.service.impl.UserServiceImpl;
import com.xzzn.pollux.utils.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 数据集信息表 前端控制器
 * </p>
 *
 * @author xzzn
 */
@RestController
@RequestMapping("/dataset")
@Slf4j
public class DatasetController {

    @Resource
    private DatasetInfoServiceImpl datasetInfoService;

    @Resource
    private UserServiceImpl userService;

    @PostMapping
    public ResultResponse<String> importDataset(
            @RequestHeader("ACCESS-KEY") String accessKey,
            @Valid @ModelAttribute DatasetImportRequest datasetImportRequest
    ) {
        MultipartFile importFile = datasetImportRequest.getImportDataset();
        List<String> tags = datasetImportRequest.getTags();
        String userId = userService.getUser(accessKey).getId();
        String datasetId = datasetInfoService.generateDataSet(userId, importFile, tags);
        return ResultUtils.success(datasetId);
    }

    @DeleteMapping
    public BaseResponse deleteDatasets(
            @RequestHeader("ACCESS-KEY") String accessKey,
            @RequestBody DeleteRequest deleteRequest
    ) {

        List<String> deleteIds = deleteRequest.getDeleteIds();
        datasetInfoService.deleteDatasets(deleteIds);
        return BaseResponse.success();
    }

    @PutMapping
    public BaseResponse renameDataset(
            @RequestHeader("ACCESS-KEY") String accessKey,
            @RequestParam(value = "datasetId", required = true) String datasetId,
            @RequestParam(value = "datasetName",required = true) String datasetName
    ) {
        datasetInfoService.renameDataset(datasetId, datasetName);
        return BaseResponse.success();
    }

    @GetMapping("/list")
    public ListResponse<ArrayList<DatasetListResponse.DatasetInfoVO>> getDatasets(
            @RequestHeader("ACCESS-KEY") String accessKey,
            @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "10") Integer size,
            @RequestParam(value = "sortAttribute", required = false, defaultValue = "createTime") String sortAttribute,
            @RequestParam(value = "sortDirection", required = false, defaultValue = "desc") String sortDirection,
            @RequestParam(value = "datasetName", required = false) String datasetName,
            @RequestParam(value = "tag", required = false) String tag,
            @RequestParam(value = "startTime", required = false) Long startTime,
            @RequestParam(value = "endTime", required = false) Long endTime
    ) {
        User user = userService.getUser(accessKey);

        DatasetListResponse datasetListResponses = datasetInfoService.getDatasetInfoList(page, size,
                sortAttribute, sortDirection,
                datasetName, tag, startTime, endTime, user.getId());
        return new ListResponse<>(200, datasetListResponses.getDatasetInfoVOList(),
                datasetListResponses.getTotal(), "success");
    }

    @PutMapping("/tags")
    public BaseResponse changeTags(
            @RequestHeader("ACCESS-KEY") String accessKey,
            @RequestBody DatasetChangeTagsRequest datasetChangeTagsRequest
    ) {
        String datasetId = datasetChangeTagsRequest.getDatasetId();
        List<String> tags = datasetChangeTagsRequest.getTags();
        datasetInfoService.changeTags(datasetId, tags);
        return BaseResponse.success();
    }

    @GetMapping("/preview")
    public ResultResponse<FilePreviewResponse> getFilePath(
            @RequestHeader("ACCESS-KEY") String accessKey,
            @RequestParam String fileId
    ) {
        return ResultUtils.success(datasetInfoService.getFilePath(fileId));
    }

    @GetMapping("/tree")
    public ResultResponse<FileTreeNode> getTree(
            @RequestHeader("ACCESS-KEY") String accessKey,
            @RequestParam String datasetId
    ) {
        return ResultUtils.success(datasetInfoService.getTree(datasetId));
    }
}
