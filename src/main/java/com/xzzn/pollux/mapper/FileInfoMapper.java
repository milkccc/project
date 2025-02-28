package com.xzzn.pollux.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xzzn.pollux.entity.FileInfo;
import org.apache.ibatis.annotations.Select;

/**
 * <p>
 * 文件信息表 Mapper 接口
 * </p>
 *
 * @author xzzn
 */
public interface FileInfoMapper extends BaseMapper<FileInfo> {
    @Select("SELECT id FROM file_info WHERE dataset_id = #{datasetId}")
    String selectIdByDatasetId(String datasetId);

    @Select("SELECT file_type FROM file_info WHERE dataset_id = #{datasetId}")
    String selectFileTypeById(String datasetId);


}
