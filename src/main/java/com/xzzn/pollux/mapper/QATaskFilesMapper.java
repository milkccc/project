package com.xzzn.pollux.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xzzn.pollux.entity.QATaskFiles;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * <p>
 * 任务文件关联表 Mapper 接口
 * </p>
 *
 * @author xzzn
 */
public interface QATaskFilesMapper extends BaseMapper<QATaskFiles> {

    @Update("UPDATE qa_task_files SET qa_count = qa_count + #{qaCount} WHERE task_id = #{taskId} and file_id = #{fileId}")
    void updateQACount(@Param("taskId") String taskId, @Param("fileId") String fileId, @Param("qaCount") int qaCount);

}
