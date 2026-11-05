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
INSERT INTO sys_menu VALUES('2101', '数据源配置', '2100', '1', 'info', 'datasource/info/index', '', '', 1, 0, 'C', '0', '0', 'datasource:info:list', 'redis', 'admin', sysdate(), '', null, '数据源配置菜单');
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