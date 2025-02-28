package com.xzzn.pollux.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xzzn.pollux.entity.QATaskFiles;
import com.xzzn.pollux.mapper.QATaskFilesMapper;
import com.xzzn.pollux.service.IQATaskFilesService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 任务文件关联表 服务实现类
 * </p>
 *
 * @author xzzn
 */
@Service
public class QATaskFilesServiceImpl extends ServiceImpl<QATaskFilesMapper, QATaskFiles> implements IQATaskFilesService {

}
