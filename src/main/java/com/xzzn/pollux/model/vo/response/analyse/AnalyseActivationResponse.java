package com.xzzn.pollux.model.vo.response.analyse;

import com.xzzn.pollux.model.pojo.ActivationInfo;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;

@Data
@AllArgsConstructor
public class AnalyseActivationResponse implements Serializable {

    /**
     * 记录不同时间维度（日、周、月、季度、年）的数据
     */
    private HashMap<String, ActivationInfo> resultMap;

    public AnalyseActivationResponse(){
        this.resultMap = new HashMap<>();
    }
}
