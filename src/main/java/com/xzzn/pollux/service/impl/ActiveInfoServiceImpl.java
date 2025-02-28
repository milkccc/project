package com.xzzn.pollux.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xzzn.pollux.entity.ActiveInfo;
import com.xzzn.pollux.mapper.ActiveInfoMapper;
import com.xzzn.pollux.service.IActiveInfoService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 活跃度信息表 服务实现类
 * </p>
 *
 * @author xzzn
 */
@Service
public class ActiveInfoServiceImpl extends ServiceImpl<ActiveInfoMapper, ActiveInfo> implements IActiveInfoService {
    @Override
    public List<ActiveInfo> getAllActiveInfo() {
        return this.lambdaQuery().orderByAsc(ActiveInfo::getDateTime).list();
    }
}
