package com.xzzn.pollux.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xzzn.pollux.entity.QATaskLocal;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * <p>
 * 本地QA对上传表 Mapper 接口
 * </p>
 *
 * @author xzzn
 */
public interface QATaskLocalMapper extends BaseMapper<QATaskLocal> {

    @Insert("INSERT INTO qa_task_local (task_id, create_time, update_time, totalqa_count) VALUES (#{taskId}, #{createTime}, #{updateTime}, #{totalqaCount})")
    int insert(QATaskLocal qaTaskLocal);

    @Select("SELECT COUNT(*) > 0 FROM qa_task WHERE id = #{taskId}")
    boolean existsTaskById(@Param("taskId") String taskId);
}



