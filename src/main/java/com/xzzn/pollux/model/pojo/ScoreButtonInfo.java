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
public class ScoreButtonInfo implements Serializable {
    private String value;
    private String icon;
    private String color;

    // 使用FastJson转为json字符串
    public String toJsonString() {
        return com.alibaba.fastjson.JSON.toJSONString(this);
    }

    // 使用FastJson将json字符串转为对象
    public static ScoreButtonInfo fromJsonString(String jsonString) {
        return com.alibaba.fastjson.JSON.parseObject(jsonString, ScoreButtonInfo.class);
    }
}
