-- 创建库
CREATE DATABASE IF NOT EXISTS pollux;


-- 切换库
USE pollux;


-- 用户表
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`
(
    `id`            varchar(32)           NOT NULL COMMENT '用户id',
    `user_name`     varchar(256)                   DEFAULT NULL COMMENT '用户昵称',
    `user_account`  varchar(256)          NOT NULL COMMENT '账号',
    `user_avatar`   varchar(1024)                  DEFAULT NULL COMMENT '用户头像',
    `gender`        tinyint                        DEFAULT NULL COMMENT '性别',
    `user_role`     enum ('user','admin') NOT NULL DEFAULT 'user' COMMENT '用户角色：user / admin',
    `user_password` varchar(512)          NOT NULL COMMENT '密码',
    `create_time`   datetime                       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   datetime                       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`     tinyint               NOT NULL DEFAULT '0' COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `user_account` (`user_account`)
) COMMENT ='用户' ENGINE = InnoDB
                  DEFAULT CHARSET = utf8mb4;


-- 用户登陆表
DROP TABLE IF EXISTS `user_login`;
CREATE TABLE `user_login`
(
    `id`                varchar(32) NOT NULL COMMENT '用户登陆id',
    `user_id`           varchar(32)          DEFAULT NULL COMMENT '用户ID',
    `login_time`        datetime             DEFAULT NULL COMMENT '登陆时间',
    `login_ip`          varchar(64)          DEFAULT NULL COMMENT 'IP',
    `login_device`      varchar(64)          DEFAULT NULL COMMENT '设备',
    `login_os`          varchar(64)          DEFAULT NULL COMMENT '操作系统',
    `login_browser`     varchar(64)          DEFAULT NULL COMMENT '浏览器',
    `login_status`      int                  DEFAULT NULL COMMENT '登录状态 0-未登录 1-已登录',
    `login_fail_reason` varchar(128)         DEFAULT NULL COMMENT '登陆失败原因',
    `create_time`       timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       timestamp   NULL     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) COMMENT ='用户登陆' ENGINE = InnoDB
                      DEFAULT CHARSET = utf8mb4;

-- 数据集信息表
DROP TABLE IF EXISTS `dataset_info`;
CREATE TABLE `dataset_info`
(
    `id`             varchar(32)  NOT NULL COMMENT '数据集id',
    `dataset_name`   varchar(256) NOT NULL COMMENT '数据集名称',
    `dataset_path`   varchar(256)          DEFAULT NULL COMMENT '数据集存储路径',
    `dataset_size`   bigint       NOT NULL COMMENT '数据集大小',
    `dataset_status` varchar(20)  NOT NULL COMMENT '数据集解析状态',
    `uploader_id`    varchar(32)  NOT NULL COMMENT '上传用户id',
    `total`          int          NOT NULL DEFAULT '0' COMMENT '文件总数',
    `complete`       int          NOT NULL DEFAULT '0' COMMENT '文件解析完成数',
    `tree`           longtext     NOT NULL COMMENT '数据集结构树',
    `tags`           varchar(256) NOT NULL COMMENT '标签',
    `create_time`    timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    `update_time`    timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) COMMENT ='数据集信息表' ENGINE = InnoDB
                          DEFAULT CHARSET = utf8mb4;


-- 文件信息表
DROP TABLE IF EXISTS `file_info`;
CREATE TABLE `file_info`
(
    `id`              varchar(32)  NOT NULL COMMENT '文件id',
    `dataset_id`      varchar(32)  NOT NULL COMMENT '数据集id',
    `file_level`      int          NOT NULL COMMENT '文件层级',
    `file_name`       varchar(256) NOT NULL COMMENT '文件名称',
    `file_path`       varchar(256) NOT NULL COMMENT '文件路径',
    `parse_file_path` varchar(256) NOT NULL COMMENT '解析文件路径',
    `file_type`       varchar(10)  NOT NULL COMMENT '文件类型',
    `file_size`       bigint       NOT NULL COMMENT '文件大小',
    `file_status`     varchar(20)  NOT NULL COMMENT '文件状态',
    `fail_reason`     varchar(256)          DEFAULT NULL COMMENT '解析失败原因',
    `create_time`     timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    `update_time`     timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `dataset_id` (`dataset_id`),
    CONSTRAINT `file_info_ibfk_1` FOREIGN KEY (`dataset_id`) REFERENCES `dataset_info` (`id`) ON DELETE CASCADE
) COMMENT ='文件信息表' ENGINE = InnoDB
                        DEFAULT CHARSET = utf8mb4;


-- 推理任务表
DROP TABLE IF EXISTS `qa_task`;
CREATE TABLE `qa_task`
(
    `id`               varchar(32) NOT NULL COMMENT '任务id',
    `task_name`        varchar(32) NOT NULL COMMENT '任务名称',
    `split_level`      int         NOT NULL COMMENT '段落精细度',
    `density_level`    int         NOT NULL COMMENT '提问密度',
    `domain`           varchar(16) NOT NULL COMMENT '提问领域',
    `description`      text        NOT NULL COMMENT '需求描述',
    `task_status`      varchar(20) NOT NULL COMMENT '任务状态',
    `task_start_time`  timestamp   NULL     DEFAULT NULL COMMENT '任务开始时间',
    `task_end_time`    timestamp   NULL     DEFAULT NULL COMMENT '任务结束时间',
    `task_creator_id`  varchar(32) NOT NULL COMMENT '任务创建者id',
    `total`            int         NOT NULL DEFAULT '0' COMMENT '文件总数',
    `complete`         int         NOT NULL DEFAULT '0' COMMENT '文件已完成数',
    `qa_count`         int         NOT NULL DEFAULT '0' COMMENT '生成QA对数量',
    `review_count`     int         NOT NULL DEFAULT '0' COMMENT '审核QA对数量',
    `review_config_id` int                  DEFAULT NULL COMMENT '审核配置id，为null则说明未设置',
    `create_time`      timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='推理任务表';


-- 任务数据集关联表
DROP TABLE IF EXISTS `qa_task_datasets`;
CREATE TABLE `qa_task_datasets`
(
    `id`         int         NOT NULL AUTO_INCREMENT COMMENT 'id',
    `task_id`    varchar(32) NOT NULL COMMENT '任务id',
    `dataset_id` varchar(32) NOT NULL COMMENT '数据集id',
    `status`     varchar(20) NOT NULL COMMENT '任务数据集状态',
    PRIMARY KEY (`id`),
    KEY `task_id` (`task_id`),
    KEY `dataset_id` (`dataset_id`),
    CONSTRAINT `qa_task_datasets_ibfk_1` FOREIGN KEY (`task_id`) REFERENCES `qa_task` (`id`) ON DELETE CASCADE,
    CONSTRAINT `qa_task_datasets_ibfk_2` FOREIGN KEY (`dataset_id`) REFERENCES `dataset_info` (`id`) ON DELETE CASCADE
) COMMENT ='任务数据集关联表' ENGINE = InnoDB
                              DEFAULT CHARSET = utf8mb4;


-- 任务文件关联表
DROP TABLE IF EXISTS `qa_task_files`;
CREATE TABLE `qa_task_files`
(
    `id`       int         NOT NULL AUTO_INCREMENT COMMENT 'id',
    `task_id`  varchar(32) NOT NULL COMMENT '任务id',
    `file_id`  varchar(32) NOT NULL COMMENT '子文件id',
    `status`   varchar(20) NOT NULL COMMENT '任务文件状态',
    `qa_count` int DEFAULT '0' COMMENT 'QA对数量',
    PRIMARY KEY (`id`),
    KEY `task_id` (`task_id`),
    KEY `file_id` (`file_id`),
    CONSTRAINT `qa_task_files_ibfk_1` FOREIGN KEY (`task_id`) REFERENCES `qa_task` (`id`) ON DELETE CASCADE,
    CONSTRAINT `qa_task_files_ibfk_2` FOREIGN KEY (`file_id`) REFERENCES `file_info` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='任务文件关联表';


-- 活跃度信息表
DROP TABLE IF EXISTS `active_info`;
CREATE TABLE `active_info`
(
    `id`            int       NOT NULL AUTO_INCREMENT COMMENT 'id',
    `date_time`     date      NOT NULL COMMENT '日期',
    `user_active`   bigint    NOT NULL COMMENT '用户日活',
    `data_generate` bigint    NOT NULL COMMENT '数据生成日活',
    `data_review`   bigint    NOT NULL COMMENT '数据审核日活',
    `create_time`   timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    `update_time`   timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) COMMENT ='活跃度信息表' ENGINE = InnoDB
                          AUTO_INCREMENT = 36
                          DEFAULT CHARSET = utf8mb4 COMMENT ='活跃度信息表';


-- 任务审核配置表
DROP TABLE IF EXISTS `review_config`;
CREATE TABLE `review_config`
(
    `id`                    int          NOT NULL AUTO_INCREMENT COMMENT 'id',
    `task_id`               varchar(32)  NOT NULL COMMENT '任务id',
    `Q_review_criteria`     text         NOT NULL COMMENT '问题审核标准',
    `A_review_criteria`     text         NOT NULL COMMENT '答案审核标准',
    `is_step_two`           tinyint(1)   NOT NULL DEFAULT '1' COMMENT '是否进行第二步审核',
    `score_review_criteria` text         NOT NULL COMMENT '打分审核标准',
    `score_button_info`     varchar(256) NOT NULL COMMENT '打分按钮信息',
    `create_time`           timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    `update_time`           timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `task_id` (`task_id`),
    CONSTRAINT `review_config_ibfk_1` FOREIGN KEY (`task_id`) REFERENCES `qa_task` (`id`) ON DELETE CASCADE
) COMMENT ='任务审核配置表' ENGINE = InnoDB
                            DEFAULT CHARSET = utf8mb4;


-- 任务审核表
DROP TABLE IF EXISTS `task_review`;
CREATE TABLE `task_review`
(
    `id`                 int         NOT NULL AUTO_INCREMENT COMMENT 'id',
    `user_id`            varchar(32) NOT NULL COMMENT '用户id',
    `task_id`            varchar(32) NOT NULL COMMENT '任务id',
    `last_child_file_id` varchar(32)          DEFAULT NULL COMMENT '上一次分配的qa对所属子文件id',
    `allocated_qa_list`  text COMMENT '当前分配的qa对id列表',
    `allocated_qa_num`   int         NOT NULL DEFAULT '0' COMMENT '当前分配的qa对审核数',
    `review_cur_num`     int         NOT NULL DEFAULT '0' COMMENT '当前批次审核数',
    `review_total_num`   int         NOT NULL DEFAULT '0' COMMENT '总审核数',
    `create_time`        timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    `update_time`        timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `task_id` (`task_id`),
    KEY `file_id` (`user_id`),
    CONSTRAINT `task_review_ibfk_1` FOREIGN KEY (`task_id`) REFERENCES `qa_task` (`id`) ON DELETE CASCADE,
    CONSTRAINT `task_review_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) COMMENT ='任务审核表' ENGINE = InnoDB
                        DEFAULT CHARSET = utf8mb4;


-- 模型信息表
DROP TABLE IF EXISTS `model_info`;
CREATE TABLE `model_info`
(
    `id`               varchar(32)  NOT NULL COMMENT '模型id',
    `model_name`       varchar(32)  NOT NULL COMMENT '模型名称',
    `model_category`   varchar(32)  NOT NULL COMMENT '模型种类,如基座模型,微调训练模型',
    `model_type`       varchar(32)  NULL COMMENT '模型类型,如文生图类型',
    `model_intro`      varchar(512) NULL COMMENT '模型简介',
    `model_scene`      varchar(512) NULL COMMENT '模型场景',
    `model_param`      varchar(32)  NULL COMMENT '模型参数',
    `model_path`      varchar(128)  NULL COMMENT '模型存放路径',
    `base_model`       varchar(32)  NULL COMMENT '基座模型',
    `user_id`          varchar(32)  NULL COMMENT '微调训练用户',
    `test_set_ratio`   double       NULL COMMENT '测试集比例',
    `train_strategy`   varchar(32)  NULL COMMENT '训练策略',
    `iteration_round`  int          NULL COMMENT '迭代轮次',
    `learning_rate`    double       NULL COMMENT '学习率',
    `batch_size`       int          NULL COMMENT '批次大小',
    `train_status`     varchar(32)  NULL COMMENT '训练状态',
    `train_log`        varchar(512) NULL COMMENT '训练日志',
    `stop_reason`      varchar(32)  NULL COMMENT '中断原因',
    `evaluate_status`  varchar(32)  NULL COMMENT '评估状态',
    `evaluate_score`   varchar(512) NULL COMMENT '评估分数',
    `last_online_time` timestamp    NULL COMMENT '上次上线时间',
    `online_time`      timestamp    NULL COMMENT '上线时间',
    `online_count`     int          NULL COMMENT '上线次数',
    `duration`         long         NULL COMMENT '上线持续时间 ',
    `publish_config`   varchar(512) NULL COMMENT '发布参数配置',
    `create_time`      timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='模型信息表';


# 建立管理员账号
insert into user (id, user_name, user_account, user_role, user_password)
values ('9fa41311373495e654bfcce77d567d51', 'admin', 'admin', 'admin', '0fc95006c8f123a6e53ce28a3ee1dccd');


# 插入日活数据
INSERT INTO active_info (id, date_time, user_active, data_generate, data_review)
VALUES (NULL, '2023-11-02', 1000, 50051, 20032),
       (NULL, '2023-11-03', 1200, 55012, 22010),
       (NULL, '2023-11-04', 1050, 48032, 18020),
       (NULL, '2023-11-05', 1150, 52051, 21084),
       (NULL, '2023-11-06', 950, 45021, 19054),
       (NULL, '2023-11-07', 1100, 51021, 20563),
       (NULL, '2023-11-08', 1300, 60042, 24065),
       (NULL, '2023-11-09', 1250, 58021, 23014),
       (NULL, '2023-11-10', 1350, 63012, 25096),
       (NULL, '2023-11-11', 1400, 65021, 26048),
       (NULL, '2023-11-12', 1150, 52032, 21065),
       (NULL, '2023-11-13', 1000, 50012, 20078),
       (NULL, '2023-11-14', 1150, 52065, 21065),
       (NULL, '2023-11-15', 1200, 55054, 22012),
       (NULL, '2023-11-16', 1300, 60087, 24032),
       (NULL, '2023-11-17', 1250, 58021, 23095),
       (NULL, '2023-11-18', 1100, 51056, 20568),
       (NULL, '2023-11-19', 1050, 48012, 18015),
       (NULL, '2023-11-20', 950, 45012, 19054),
       (NULL, '2023-11-21', 1150, 52065, 21035),
       (NULL, '2023-11-22', 1350, 63068, 25012),
       (NULL, '2023-11-23', 1400, 65092, 26012),
       (NULL, '2023-11-24', 1100, 51012, 20578),
       (NULL, '2023-11-25', 1050, 48032, 18020),
       (NULL, '2023-11-26', 1150, 52051, 21084),
       (NULL, '2023-11-27', 1204, 45021, 19054),
       (NULL, '2023-11-28', 1200, 55021, 22063),
       (NULL, '2023-11-29', 1300, 60042, 24065),
       (NULL, '2023-11-30', 1250, 58021, 23014),
       (NULL, '2023-12-01', 1350, 63012, 25096),
       (NULL, '2023-12-02', 1400, 65021, 26048),
       (NULL, '2023-12-03', 1150, 52032, 21065),
       (NULL, '2023-12-04', 1000, 50012, 20078),
       (NULL, '2023-12-05', 1250, 52132, 20065),
       (NULL, '2023-12-06', 1150, 52051, 21365);