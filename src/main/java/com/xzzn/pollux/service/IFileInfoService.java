package com.xzzn.pollux.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xzzn.pollux.entity.FileInfo;

/**
 * <p>
 * 文件信息表 服务类
 * </p>
 *
 * @author xzzn
 */
public interface IFileInfoService extends IService<FileInfo> {

    void saveFileInfo(FileInfo fileInfo, Integer retries);
}
