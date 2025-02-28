package com.xzzn.pollux.model.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ActivationInfo implements Serializable {
    private String[] dateList;
    private Long[] numList;
}