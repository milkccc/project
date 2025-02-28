package com.xzzn.pollux.model.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskConfigMap {
    // 分割等级
    private Integer splitLevel;

    // 问题密度
    private Integer questionDensity;
    // 创建默认配置的静态方法
    public static TaskConfigMap defaultConfig() {
        return TaskConfigMap.builder()
                .splitLevel(3) // 默认分割等级
                .questionDensity(3) // 默认问题密度
                .build();
    }
}