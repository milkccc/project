package com.xzzn.pollux.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xzzn.pollux.entity.FileInfo;
import com.xzzn.pollux.mapper.FileInfoMapper;
import com.xzzn.pollux.service.IFileInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 文件信息表 服务实现类
 * </p>
 *
 * @author xzzn
 */
@Slf4j
@Service
public class FileInfoServiceImpl extends ServiceImpl<FileInfoMapper, FileInfo> implements IFileInfoService {

    @Override
    public void saveFileInfo(FileInfo fileInfo, Integer retries) {
        boolean succeed = this.save(fileInfo);
        if (!succeed) {
            if (retries > 0) {
                saveFileInfo(fileInfo, retries - 1);
            } else {
                log.error("保存子文件信息 {} 失败", fileInfo);
            }
        } else {
            log.info("保存子文件信息成功");
        }
    }
}
