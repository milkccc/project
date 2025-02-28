package com.xzzn.pollux.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xzzn.pollux.entity.ModelInfo;
import org.apache.ibatis.annotations.Select;

public interface ModelInfoMapper extends BaseMapper<ModelInfo> {
    @Select("SELECT SUM(online_count) FROM model_info")
    Integer selectTotalOnlineCount();
}
