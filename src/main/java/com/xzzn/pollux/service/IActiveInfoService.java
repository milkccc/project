package com.xzzn.pollux.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xzzn.pollux.entity.ActiveInfo;

import java.util.List;

/**
 * <p>
 * 活跃度信息表 服务类
 * </p>
 *
 * @author xzzn
 */
public interface IActiveInfoService extends IService<ActiveInfo> {

    List<ActiveInfo> getAllActiveInfo();
}
