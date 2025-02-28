package com.xzzn.pollux.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xzzn.pollux.entity.TaskReview;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * <p>
 * 任务审核表 Mapper 接口
 * </p>
 *
 * @author xzzn
 */
public interface TaskReviewMapper extends BaseMapper<TaskReview> {
    @Update("UPDATE task_review SET review_cur_num = review_cur_num + #{reviewCount}, review_total_num = review_total_num + #{reviewCount} WHERE task_id = #{taskId} AND user_id = #{userId} ")
    void updateReviewCount(@Param("taskId") String taskId, @Param("userId") String userId, @Param("reviewCount") int reviewCount);

    @Select("SELECT SUM(review_total_num) FROM task_review WHERE user_id = #{userId}")
    Integer getTotalReviewNumByUserId(@Param("userId") String userId);
}
