-- 创建库
CREATE DATABASE IF NOT EXISTS pollux;

-- 切换库
USE pollux;

-- 本地QA对上传表
DROP TABLE IF EXISTS `qa_task_local`;
CREATE TABLE `qa_task_local`
(
    `id`              int         NOT NULL AUTO_INCREMENT COMMENT 'id',
    `task_id`         varchar(32)  NOT NULL COMMENT '任务id',
    `totalqa_count`    int          NOT NULL COMMENT 'QA数量',
    `create_time`     timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     timestamp    NULL     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    CONSTRAINT `qa_task_qa_ibfk_1` FOREIGN KEY (`task_id`) REFERENCES `qa_task` (`id`) ON DELETE CASCADE
) COMMENT ='本地QA对上传表' ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;


