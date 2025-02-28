package com.xzzn.pollux.model.vo.request.task;

import com.xzzn.pollux.model.pojo.TaskConfigMap;
import lombok.Data;
import org.springframework.stereotype.Component;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;


@Component
@Data
public class QATaskAutoCreateRequest {
    private String taskName;

    private TaskConfigMap taskConfigMap;

    private String domain;

    private String description;

    private boolean priority;  //是否生成小样QA对

//    @ApiModelProperty(value = "file")
//    @NotNull
//    //private MultipartFile file;

    public QATaskAutoCreateRequest() {

        this.taskName = "自动任务-" + generateQATaskId();

        this.taskConfigMap = TaskConfigMap.defaultConfig(); // 使用静态方法创建默认配置

        this.domain = "";

        this.description = "";

        this.priority = true; // 将 priority 默认为 true
    }
    private static final Random random = new Random();

    public String generateQATaskId() {
        // 定义日期格式
        String pattern = "yyyyMMddHHmmss";
        SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);

        // 获取当前日期时间
        String currentDate = dateFormat.format(new Date());

        // 递增编号
        int newNumber = 1000 + random.nextInt(9000);

        // 构造最终的主键
        return currentDate + String.format("%04d", newNumber);
    }

}
