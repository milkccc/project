package com.xzzn.pollux.model.vo.response.model;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.xzzn.pollux.model.vo.response.dataset.DatasetListResponse;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ModelListResponse implements Serializable{
    private ArrayList<ModelInfoVO> modelInfoVOList;

    private long total=0L;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModelInfoVO implements Serializable {

        private String id;
        private String modelName;
        private String modelStatus;
        private String modelIntro;
        private String uploaderId;
        private Integer total;
    }
}
