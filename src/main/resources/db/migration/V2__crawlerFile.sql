-- 创建库
CREATE DATABASE IF NOT EXISTS pollux;

-- 切换库
USE pollux;

-- 爬虫信息表
DROP TABLE IF EXISTS `page_hashes`;


CREATE TABLE page_hashes (
                             `url`              VARCHAR(768) PRIMARY KEY,
                             `content_hash`     VARCHAR(64) NOT NULL,
                             `last_updated`     DATETIME NOT NULL
) COMMENT ='爬虫信息表' ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

