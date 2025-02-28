package com.xzzn.pollux.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xzzn.pollux.entity.QATask;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * <p>
 * 推理任务表 Mapper 接口
 * </p>
 *
 * @author xzzn
 */
public interface QATaskMapper extends BaseMapper<QATask> {


    @Update("UPDATE qa_task SET complete = complete + 1 WHERE id = #{taskId}")
    void incrementComplete(@Param("taskId") String taskId);

    @Update("UPDATE qa_task SET qa_count = qa_count + #{qaCount} WHERE id = #{taskId}")
    void updateQACount(@Param("taskId") String taskId, @Param("qaCount") int qaCount);

    @Update("UPDATE qa_task SET review_count = review_count + #{reviewCount} WHERE id = #{taskId}")
    void updateReviewCount(@Param("taskId") String taskId, @Param("reviewCount") int reviewCount);

    @Select("SELECT COALESCE(MAX(CAST(SUBSTRING(id, 9) AS SIGNED)), 0) FROM qa_task WHERE id LIKE CONCAT(#{datePrefix}, '%')")
    int getCurrentMaxNumber(@Param("datePrefix") String datePrefix);

    @Update("UPDATE qa_task SET complete = complete + #{size} WHERE id = #{taskId}")
    void updateCompleteCount(@Param("taskId") String taskId, @Param("size") int size);

    @Update("UPDATE qa_task SET total = total + #{size}, complete = complete + #{size} WHERE id = #{taskId}")
    void updateTotalCount(@Param("taskId") String taskId, @Param("size") int size);

    @Update("UPDATE qa_task SET total = total + #{size} WHERE id = #{taskId}")
    void updateonlyTotalCount(@Param("taskId") String taskId, @Param("size") int size);

    @Update("UPDATE qa_task SET total = total - 1, complete = complete - 1 WHERE id = #{taskId}")
    void deleteTotalandCompleteCount(@Param("taskId") String taskId);
}
