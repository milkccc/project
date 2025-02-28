package com.xzzn.pollux.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xzzn.pollux.entity.DatasetInfo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * <p>
 * 数据集信息表 Mapper 接口
 * </p>
 *
 * @author xzzn
 */
public interface DatasetInfoMapper extends BaseMapper<DatasetInfo> {
    @Update("UPDATE dataset_info SET complete = complete + 1 WHERE id = #{datasetId}")
    void incrementComplete(@Param("datasetId") String datasetId);

    @Update("UPDATE dataset_info SET dataset_status = 'SUCCESS' WHERE id = #{datasetId} AND complete = total")
    void updateStatus(@Param("datasetId") String datasetId);

    @Update("UPDATE dataset_info SET complete =  1 WHERE id = #{datasetId}")
    void CompleteSetone(@Param("datasetId") String datasetId);
}
