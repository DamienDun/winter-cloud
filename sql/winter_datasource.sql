-- ----------------------------
-- 数据源管理（对齐 datax-web job_jdbc_datasource，表前缀 ds_）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `ds_jdbc_datasource` (
  `id` bigint(20) NOT NULL COMMENT '主键',
  `datasource_name` varchar(200) NOT NULL COMMENT '数据源名称',
  `datasource_group` varchar(200) DEFAULT 'Default' COMMENT '数据源分组',
  `datasource_type` varchar(45) NOT NULL COMMENT '类型：mysql/oracle/postgresql/sqlserver/hive/clickhouse/presto',
  `jdbc_username` varchar(100) DEFAULT NULL COMMENT '用户名',
  `jdbc_password` varchar(500) DEFAULT NULL COMMENT '密码（AES加密）',
  `jdbc_url` varchar(500) DEFAULT NULL COMMENT 'jdbc url',
  `jdbc_driver_class` varchar(200) DEFAULT NULL COMMENT 'jdbc驱动类',
  `database_name` varchar(200) DEFAULT NULL COMMENT '数据库名',
  `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '状态：0停用 1启用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
  `gmt_create` datetime DEFAULT NULL COMMENT '创建时间',
  `gmt_modified` datetime DEFAULT NULL COMMENT '修改时间',
  `gmt_delete` datetime DEFAULT NULL COMMENT '删除时间',
  `created_user_id` bigint(20) DEFAULT NULL COMMENT '创建人',
  `modified_user_id` bigint(20) DEFAULT NULL COMMENT '更新人',
  `deleted_user_id` bigint(20) DEFAULT NULL COMMENT '删除人',
  PRIMARY KEY (`id`),
  KEY `idx_ds_name` (`datasource_name`),
  KEY `idx_ds_type` (`datasource_type`),
  KEY `idx_ds_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='JDBC数据源配置';

-- ----------------------------
-- 菜单：数据源管理（顶级目录，与前端常量路由「文件管理」同级）
-- ----------------------------
INSERT INTO sys_menu VALUES('2100', '数据源管理', '0', '0', 'datasource', null, '', '', 1, 0, 'M', '0', '0', '', 'redis', 'admin', sysdate(), '', null, '数据源管理目录');
INSERT INTO sys_menu VALUES('2101', 'JDBC数据源', '2100', '1', 'info', 'datasource/info/index', '', '', 1, 0, 'C', '0', '0', 'datasource:info:list', 'redis', 'admin', sysdate(), '', null, 'JDBC数据源菜单');
INSERT INTO sys_menu VALUES('2102', '数据源查询', '2101', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'datasource:info:query', '#', 'admin', sysdate(), '', null, '');
INSERT INTO sys_menu VALUES('2103', '数据源新增', '2101', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'datasource:info:add', '#', 'admin', sysdate(), '', null, '');
INSERT INTO sys_menu VALUES('2104', '数据源修改', '2101', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'datasource:info:edit', '#', 'admin', sysdate(), '', null, '');
INSERT INTO sys_menu VALUES('2105', '数据源删除', '2101', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'datasource:info:remove', '#', 'admin', sysdate(), '', null, '');
INSERT INTO sys_menu VALUES('2106', '连接测试', '2101', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'datasource:info:test', '#', 'admin', sysdate(), '', null, '');
INSERT INTO sys_role_menu VALUES ('1', '2100');
INSERT INTO sys_role_menu VALUES ('1', '2101');
INSERT INTO sys_role_menu VALUES ('1', '2102');
INSERT INTO sys_role_menu VALUES ('1', '2103');
INSERT INTO sys_role_menu VALUES ('1', '2104');
INSERT INTO sys_role_menu VALUES ('1', '2105');
INSERT INTO sys_role_menu VALUES ('1', '2106');

-- 表数据维护（挂数据源管理目录）
INSERT INTO sys_menu VALUES('2110', '表数据维护', '2100', '2', 'table', 'datasource/table/index', '', '', 1, 0, 'C', '0', '0', 'datasource:data:list', 'table', 'admin', sysdate(), '', null, '远程库表数据 CRUD');
INSERT INTO sys_menu VALUES('2111', '表数据查询', '2110', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'datasource:data:query', '#', 'admin', sysdate(), '', null, '');
INSERT INTO sys_menu VALUES('2112', '表数据新增', '2110', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'datasource:data:add', '#', 'admin', sysdate(), '', null, '');
INSERT INTO sys_menu VALUES('2113', '表数据修改', '2110', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'datasource:data:edit', '#', 'admin', sysdate(), '', null, '');
INSERT INTO sys_menu VALUES('2114', '表数据删除', '2110', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'datasource:data:remove', '#', 'admin', sysdate(), '', null, '');
INSERT INTO sys_role_menu VALUES ('1', '2110');
INSERT INTO sys_role_menu VALUES ('1', '2111');
INSERT INTO sys_role_menu VALUES ('1', '2112');
INSERT INTO sys_role_menu VALUES ('1', '2113');
INSERT INTO sys_role_menu VALUES ('1', '2114');

-- 表结构 DDL 权限（挂在表数据维护 2110 下）
INSERT INTO sys_menu VALUES('2120', '表结构查询', '2110', '10', '', '', '', '', 1, 0, 'F', '0', '0', 'datasource:schema:list', '#', 'admin', sysdate(), '', null, '');
INSERT INTO sys_menu VALUES('2121', '建表', '2110', '11', '', '', '', '', 1, 0, 'F', '0', '0', 'datasource:schema:add', '#', 'admin', sysdate(), '', null, '');
INSERT INTO sys_menu VALUES('2122', '改字段/索引', '2110', '12', '', '', '', '', 1, 0, 'F', '0', '0', 'datasource:schema:edit', '#', 'admin', sysdate(), '', null, '');
INSERT INTO sys_menu VALUES('2123', '删表', '2110', '13', '', '', '', '', 1, 0, 'F', '0', '0', 'datasource:schema:remove', '#', 'admin', sysdate(), '', null, '');
INSERT INTO sys_role_menu VALUES ('1', '2120');
INSERT INTO sys_role_menu VALUES ('1', '2121');
INSERT INTO sys_role_menu VALUES ('1', '2122');
INSERT INTO sys_role_menu VALUES ('1', '2123');

-- SQL 查询权限（挂在表数据维护 2110 下，页面内「SQL查询」页签使用）
INSERT INTO sys_menu VALUES('2131', 'SQL查询', '2110', '14', '', '', '', '', 1, 0, 'F', '0', '0', 'datasource:sql:query', '#', 'admin', sysdate(), '', null, '自定义只读 SQL 查询');
INSERT INTO sys_role_menu VALUES ('1', '2131');

-- 若曾插入独立菜单 2130，可清理：
-- DELETE FROM sys_role_menu WHERE menu_id = 2130;
-- DELETE FROM sys_menu WHERE menu_id IN (2130);


-- ----------------------------
-- 作业信息 / 作业日志 / 作业统计（表前缀 ds_，调度绑定 xxl_job_id）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `ds_job_info` (
  `id` bigint(20) NOT NULL COMMENT '主键',
  `xxl_job_id` bigint(20) DEFAULT NULL COMMENT '绑定的 xxl_job_info.id',
  `job_name` varchar(200) NOT NULL COMMENT '作业名称',
  `job_status` tinyint(4) NOT NULL DEFAULT 0 COMMENT '0停用 1开启 2过期',
  `last_execute_status` tinyint(4) NOT NULL DEFAULT -1 COMMENT '-1未执行 0执行中 1成功 2失败 3异常',
  `last_execute_time` datetime DEFAULT NULL COMMENT '最近执行时间',
  `datasource_id` bigint(20) NOT NULL COMMENT '数据源id',
  `datasource_name` varchar(200) DEFAULT NULL COMMENT '数据源名称',
  `datasource_type` varchar(45) DEFAULT NULL COMMENT '数据源类型',
  `source_table` varchar(200) DEFAULT NULL COMMENT '源表（JDBC源）',
  `target_datasource_id` bigint(20) DEFAULT NULL COMMENT '目标数据源id',
  `target_datasource_name` varchar(200) DEFAULT NULL COMMENT '目标数据源名称',
  `target_table` varchar(200) DEFAULT NULL COMMENT '目标数据表',
  `field_mapping_json` mediumtext COMMENT '字段映射JSON',
  `sync_type` tinyint(4) NOT NULL DEFAULT 0 COMMENT '0全量 1增量',
  `trigger_type` tinyint(4) NOT NULL DEFAULT 0 COMMENT '0手动 1定时',
  `cron` varchar(128) DEFAULT '' COMMENT 'cron表达式',
  `cron_setting` varchar(500) DEFAULT NULL COMMENT 'cron配置JSON',
  `effective_time_begin` datetime DEFAULT NULL COMMENT '生效开始',
  `effective_time_end` datetime DEFAULT NULL COMMENT '生效结束',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `gmt_create` datetime DEFAULT NULL,
  `gmt_modified` datetime DEFAULT NULL,
  `gmt_delete` datetime DEFAULT NULL,
  `created_user_id` bigint(20) DEFAULT NULL,
  `modified_user_id` bigint(20) DEFAULT NULL,
  `deleted_user_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_ds_job_xxl` (`xxl_job_id`),
  KEY `idx_ds_job_ds` (`datasource_id`),
  KEY `idx_ds_job_target_ds` (`target_datasource_id`),
  KEY `idx_ds_job_status` (`job_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据源作业信息';

CREATE TABLE IF NOT EXISTS `ds_job_log` (
  `id` bigint(20) NOT NULL COMMENT '主键',
  `job_id` bigint(20) NOT NULL COMMENT '作业id',
  `job_name` varchar(200) DEFAULT NULL COMMENT '作业名称',
  `trigger_type` tinyint(4) NOT NULL DEFAULT 0 COMMENT '0手动 1定时',
  `trigger_user_id` bigint(20) DEFAULT NULL COMMENT '触发人id',
  `trigger_username` varchar(64) DEFAULT NULL COMMENT '触发人名称',
  `execute_status` tinyint(4) NOT NULL DEFAULT 0 COMMENT '0执行中 1成功 2失败',
  `execute_begin_time` datetime DEFAULT NULL,
  `execute_end_time` datetime DEFAULT NULL,
  `time_consuming` varchar(64) DEFAULT NULL COMMENT '耗时',
  `handle_data_total` int(11) NOT NULL DEFAULT 0,
  `error_data_total` int(11) NOT NULL DEFAULT 0,
  `actual_update_data_total` int(11) NOT NULL DEFAULT 0,
  `job_message` text COMMENT '日志信息',
  `datasource_type` varchar(45) DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0,
  `gmt_create` datetime DEFAULT NULL,
  `gmt_modified` datetime DEFAULT NULL,
  `gmt_delete` datetime DEFAULT NULL,
  `created_user_id` bigint(20) DEFAULT NULL,
  `modified_user_id` bigint(20) DEFAULT NULL,
  `deleted_user_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_ds_job_log_job` (`job_id`),
  KEY `idx_ds_job_log_begin` (`execute_begin_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据源作业日志';

CREATE TABLE IF NOT EXISTS `ds_job_statistics` (
  `id` bigint(20) NOT NULL COMMENT '主键',
  `date_time` date NOT NULL COMMENT '统计日期',
  `pending_job` int(11) NOT NULL DEFAULT 0 COMMENT '待执行任务',
  `executed` int(11) NOT NULL DEFAULT 0 COMMENT '已执行次数',
  `failed_job` int(11) NOT NULL DEFAULT 0 COMMENT '失败作业个数',
  `failed_execution` int(11) NOT NULL DEFAULT 0 COMMENT '失败执行次数',
  `handle_data_total` int(11) NOT NULL DEFAULT 0,
  `actual_update_data_total` int(11) NOT NULL DEFAULT 0,
  `error_data_total` int(11) NOT NULL DEFAULT 0,
  `remark` varchar(500) DEFAULT NULL,
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0,
  `gmt_create` datetime DEFAULT NULL,
  `gmt_modified` datetime DEFAULT NULL,
  `gmt_delete` datetime DEFAULT NULL,
  `created_user_id` bigint(20) DEFAULT NULL,
  `modified_user_id` bigint(20) DEFAULT NULL,
  `deleted_user_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ds_job_stats_date` (`date_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据源作业统计';

-- 菜单：作业信息 / 作业日志 / 作业统计
INSERT INTO sys_menu VALUES('2140', '作业信息', '2100', '3', 'job', 'datasource/job/index', '', '', 1, 0, 'C', '0', '0', 'datasource:job:list', 'job', 'admin', sysdate(), '', null, '数据源作业信息');
INSERT INTO sys_menu VALUES('2141', '作业查询', '2140', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'datasource:job:query', '#', 'admin', sysdate(), '', null, '');
INSERT INTO sys_menu VALUES('2142', '作业新增', '2140', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'datasource:job:add', '#', 'admin', sysdate(), '', null, '');
INSERT INTO sys_menu VALUES('2143', '作业修改', '2140', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'datasource:job:edit', '#', 'admin', sysdate(), '', null, '');
INSERT INTO sys_menu VALUES('2144', '作业删除', '2140', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'datasource:job:remove', '#', 'admin', sysdate(), '', null, '');
INSERT INTO sys_menu VALUES('2145', '作业启停', '2140', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'datasource:job:changeStatus', '#', 'admin', sysdate(), '', null, '');
INSERT INTO sys_menu VALUES('2146', '作业执行', '2140', '6', '', '', '', '', 1, 0, 'F', '0', '0', 'datasource:job:execute', '#', 'admin', sysdate(), '', null, '');
INSERT INTO sys_menu VALUES('2150', '作业日志', '2100', '4', 'jobLog', 'datasource/jobLog/index', '', '', 1, 0, 'C', '0', '0', 'datasource:jobLog:list', 'log', 'admin', sysdate(), '', null, '数据源作业日志');
INSERT INTO sys_menu VALUES('2151', '日志查询', '2150', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'datasource:jobLog:query', '#', 'admin', sysdate(), '', null, '');
INSERT INTO sys_menu VALUES('2152', '日志删除', '2150', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'datasource:jobLog:remove', '#', 'admin', sysdate(), '', null, '');
INSERT INTO sys_menu VALUES('2160', '作业统计', '2100', '5', 'jobStats', 'datasource/jobStats/index', '', '', 1, 0, 'C', '0', '0', 'datasource:jobStats:list', 'chart', 'admin', sysdate(), '', null, '数据源作业统计');
INSERT INTO sys_menu VALUES('2161', '统计查询', '2160', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'datasource:jobStats:query', '#', 'admin', sysdate(), '', null, '');
INSERT INTO sys_role_menu VALUES ('1', '2140');
INSERT INTO sys_role_menu VALUES ('1', '2141');
INSERT INTO sys_role_menu VALUES ('1', '2142');
INSERT INTO sys_role_menu VALUES ('1', '2143');
INSERT INTO sys_role_menu VALUES ('1', '2144');
INSERT INTO sys_role_menu VALUES ('1', '2145');
INSERT INTO sys_role_menu VALUES ('1', '2146');
INSERT INTO sys_role_menu VALUES ('1', '2150');
INSERT INTO sys_role_menu VALUES ('1', '2151');
INSERT INTO sys_role_menu VALUES ('1', '2152');
INSERT INTO sys_role_menu VALUES ('1', '2160');
INSERT INTO sys_role_menu VALUES ('1', '2161');


-- ----------------------------
-- API数据源（ds_api_datasource）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `ds_api_datasource` (
  `id` bigint(20) NOT NULL COMMENT '主键',
  `datasource_name` varchar(100) NOT NULL COMMENT '数据源名称',
  `datasource_group` varchar(200) DEFAULT 'Default' COMMENT '数据源分组',
  `api_url` varchar(512) NOT NULL COMMENT '接口地址',
  `req_type` tinyint(4) NOT NULL DEFAULT 1 COMMENT '请求方式：0 POST，1 GET',
  `data_item_key` varchar(512) DEFAULT NULL COMMENT '数据项KEY（响应取值路径）',
  `req_params_json` mediumtext COMMENT '请求参数JSON',
  `rely_datasource_id` bigint(20) DEFAULT NULL COMMENT '依赖API数据源ID',
  `belong_to_rely_datasource` tinyint(4) NOT NULL DEFAULT 1 COMMENT '是否依赖数据源：0是，1否',
  `datasource_desc` varchar(200) DEFAULT NULL COMMENT '数据源描述',
  `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '状态：0停用，1启用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否，1是',
  `gmt_create` datetime DEFAULT NULL COMMENT '创建时间',
  `gmt_modified` datetime DEFAULT NULL COMMENT '修改时间',
  `gmt_delete` datetime DEFAULT NULL COMMENT '删除时间',
  `created_user_id` bigint(20) DEFAULT NULL COMMENT '创建人',
  `modified_user_id` bigint(20) DEFAULT NULL COMMENT '更新人',
  `deleted_user_id` bigint(20) DEFAULT NULL COMMENT '删除人',
  PRIMARY KEY (`id`),
  KEY `idx_ds_api_name` (`datasource_name`),
  KEY `idx_ds_api_group` (`datasource_group`),
  KEY `idx_ds_api_rely` (`rely_datasource_id`),
  KEY `idx_ds_api_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API数据源配置';

-- 已有库增量：补分组字段 + 字段注释中文化（表已存在时执行）
-- ALTER TABLE `ds_api_datasource` ADD COLUMN `datasource_group` varchar(200) DEFAULT 'Default' COMMENT '数据源分组' AFTER `datasource_name`;
-- ALTER TABLE `ds_api_datasource` ADD INDEX `idx_ds_api_group` (`datasource_group`);
-- ALTER TABLE `ds_api_datasource`
--   MODIFY COLUMN `id` bigint(20) NOT NULL COMMENT '主键',
--   MODIFY COLUMN `datasource_name` varchar(100) NOT NULL COMMENT '数据源名称',
--   MODIFY COLUMN `api_url` varchar(512) NOT NULL COMMENT '接口地址',
--   MODIFY COLUMN `req_type` tinyint(4) NOT NULL DEFAULT 1 COMMENT '请求方式：0 POST，1 GET',
--   MODIFY COLUMN `data_item_key` varchar(512) DEFAULT NULL COMMENT '数据项KEY（响应取值路径）',
--   MODIFY COLUMN `req_params_json` mediumtext COMMENT '请求参数JSON',
--   MODIFY COLUMN `rely_datasource_id` bigint(20) DEFAULT NULL COMMENT '依赖API数据源ID',
--   MODIFY COLUMN `belong_to_rely_datasource` tinyint(4) NOT NULL DEFAULT 1 COMMENT '是否依赖数据源：0是，1否',
--   MODIFY COLUMN `datasource_desc` varchar(200) DEFAULT NULL COMMENT '数据源描述',
--   MODIFY COLUMN `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '状态：0停用，1启用',
--   MODIFY COLUMN `remark` varchar(500) DEFAULT NULL COMMENT '备注',
--   MODIFY COLUMN `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否，1是',
--   MODIFY COLUMN `gmt_create` datetime DEFAULT NULL COMMENT '创建时间',
--   MODIFY COLUMN `gmt_modified` datetime DEFAULT NULL COMMENT '修改时间',
--   MODIFY COLUMN `gmt_delete` datetime DEFAULT NULL COMMENT '删除时间',
--   MODIFY COLUMN `created_user_id` bigint(20) DEFAULT NULL COMMENT '创建人',
--   MODIFY COLUMN `modified_user_id` bigint(20) DEFAULT NULL COMMENT '更新人',
--   MODIFY COLUMN `deleted_user_id` bigint(20) DEFAULT NULL COMMENT '删除人',
--   COMMENT='API数据源配置';

INSERT INTO sys_menu VALUES('2170', 'API数据源', '2100', '2', 'api', 'datasource/api/index', '', '', 1, 0, 'C', '0', '0', 'datasource:api:list', 'link', 'admin', sysdate(), '', null, 'API datasource');
INSERT INTO sys_menu VALUES('2171', 'API数据源查询', '2170', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'datasource:api:query', '#', 'admin', sysdate(), '', null, '');
INSERT INTO sys_menu VALUES('2172', 'API数据源新增', '2170', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'datasource:api:add', '#', 'admin', sysdate(), '', null, '');
INSERT INTO sys_menu VALUES('2173', 'API数据源修改', '2170', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'datasource:api:edit', '#', 'admin', sysdate(), '', null, '');
INSERT INTO sys_menu VALUES('2174', 'API数据源删除', '2170', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'datasource:api:remove', '#', 'admin', sysdate(), '', null, '');
INSERT INTO sys_menu VALUES('2175', 'API连接测试', '2170', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'datasource:api:test', '#', 'admin', sysdate(), '', null, '');
INSERT INTO sys_role_menu VALUES ('1', '2170');
INSERT INTO sys_role_menu VALUES ('1', '2171');
INSERT INTO sys_role_menu VALUES ('1', '2172');
INSERT INTO sys_role_menu VALUES ('1', '2173');
INSERT INTO sys_role_menu VALUES ('1', '2174');
INSERT INTO sys_role_menu VALUES ('1', '2175');
