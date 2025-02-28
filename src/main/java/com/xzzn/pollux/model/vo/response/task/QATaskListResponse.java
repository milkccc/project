package com.xzzn.pollux.model.vo.response.task;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.xzzn.pollux.model.vo.response.user.UserInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


/**
 * 查询QA任务响应
 *
 * @author xzzn
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QATaskListResponse {
    private ArrayList<QATaskInfoVO> qaTaskInfoVOList;

    private Long total;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class QATaskInfoVO implements Serializable {
        private String taskId;

        private String taskName;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:SS")
        private LocalDateTime createTime;

        private String status;

        private Integer total;

        private Integer complete;

        private Integer qaCount;

        private Integer reviewCount;

        private UserInfo creator;

        private List<UserInfo> reviewers;
    }
}
