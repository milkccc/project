package com.xzzn.pollux.model.vo.response.analyse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AnalyseOverviewResponse implements Serializable {
    private long totalUserNum;
    private long onlineUserNum;
    private long dataGenerateNum;
    private long dataReviewNum;
}
