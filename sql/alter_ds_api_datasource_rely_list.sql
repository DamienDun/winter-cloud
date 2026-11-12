-- 已有库升级：API 数据源支持多依赖（有序别名列表）
-- MySQL 5.7 无 IF NOT EXISTS，重复执行会报列已存在，可忽略
ALTER TABLE `ds_api_datasource`
  ADD COLUMN `rely_list_json` text NULL COMMENT '依赖API列表JSON：[{alias,datasourceId,sort}]' AFTER `rely_datasource_id`;
