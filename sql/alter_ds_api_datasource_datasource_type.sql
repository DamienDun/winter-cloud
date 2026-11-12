-- 已有库升级：API 数据源增加类型字段 datasource_type（旧数据默认 HTTP）
-- MySQL 5.7 无 IF NOT EXISTS，重复执行会报列已存在，可忽略
ALTER TABLE `ds_api_datasource`
  ADD COLUMN `datasource_type` varchar(32) NOT NULL DEFAULT 'HTTP' COMMENT '数据源类型：HTTP / WEBSERVICE' AFTER `datasource_group`;

-- 若之前已加过 api_type，可改为：
-- ALTER TABLE `ds_api_datasource` CHANGE COLUMN `api_type` `datasource_type` varchar(32) NOT NULL DEFAULT 'HTTP' COMMENT '数据源类型：HTTP / WEBSERVICE';
