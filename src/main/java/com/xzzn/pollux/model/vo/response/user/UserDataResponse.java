package com.xzzn.pollux.model.vo.response.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserDataResponse implements Serializable {

    private int qaTotalNum;

    private int reviewTotalNum;

    private int modelTotalNum;

    private int modelOnlineTotalNum;
}