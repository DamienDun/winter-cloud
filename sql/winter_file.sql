-- ----------------------------
-- 文件服务相关表（winter-file）
-- ----------------------------

CREATE TABLE IF NOT EXISTS `fs_big_file_upload_task` (
  `id` bigint(20) NOT NULL COMMENT '主键',
  `upload_id` varchar(100) DEFAULT NULL COMMENT '分片上传uploadId',
  `channel_id` varchar(50) DEFAULT NULL COMMENT '存储通道ID',
  `file_md5` varchar(64) DEFAULT NULL COMMENT '文件MD5',
  `file_type` varchar(50) DEFAULT NULL COMMENT '文件类型/扩展名',
  `bucket_name` varchar(100) DEFAULT NULL COMMENT '桶名',
  `file_path` varchar(500) DEFAULT NULL COMMENT '对象路径',
  `access_path` varchar(500) DEFAULT NULL COMMENT '访问URL',
  `total_bytes` bigint(20) DEFAULT NULL COMMENT '总字节数',
  `complete_bytes` bigint(20) DEFAULT NULL COMMENT '已完成字节数',
  `part_bytes` bigint(20) DEFAULT NULL COMMENT '分片字节数',
  `part_count` int(11) DEFAULT NULL COMMENT '分片总数',
  `uploaded_part_numbers` text COMMENT '已上传分片编号',
  `status` int(11) DEFAULT NULL COMMENT '状态:0取消;1新建;2上传中;3完成',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_file_md5` (`file_md5`),
  KEY `idx_upload_id` (`upload_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='大文件分片上传任务';

CREATE TABLE IF NOT EXISTS `fs_resource_file` (
  `id` bigint(20) NOT NULL COMMENT '主键',
  `origin_name` varchar(255) DEFAULT NULL COMMENT '原始文件名',
  `file_type` int(11) DEFAULT NULL COMMENT '文件类型;0其他;1图片;2音频;3视频;4pdf;5doc;6excel;7ppt;8txt;9html',
  `path` varchar(1000) DEFAULT NULL COMMENT '访问路径/完整URL',
  `relative_path` varchar(500) DEFAULT NULL COMMENT '相对对象路径',
  `length` bigint(20) DEFAULT 0 COMMENT '文件大小(字节)',
  `module` varchar(255) DEFAULT NULL COMMENT '业务模块',
  `file_md5` varchar(64) DEFAULT NULL COMMENT '文件MD5',
  `apply_upload_count` int(11) DEFAULT 1 COMMENT '申请上传次数',
  `channel_id` varchar(50) DEFAULT NULL COMMENT '存储通道ID',
  `bucket_name` varchar(100) DEFAULT NULL COMMENT '桶名',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `is_deleted` tinyint(4) DEFAULT 0 COMMENT '是否删除;0未删;1已删',
  `gmt_create` datetime DEFAULT NULL COMMENT '创建时间',
  `gmt_modified` datetime DEFAULT NULL COMMENT '更新时间',
  `gmt_delete` datetime DEFAULT NULL COMMENT '删除时间',
  `created_user_id` bigint(20) DEFAULT NULL COMMENT '创建人',
  `modified_user_id` bigint(20) DEFAULT NULL COMMENT '更新人',
  `deleted_user_id` bigint(20) DEFAULT NULL COMMENT '删除人',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_file_md5` (`file_md5`),
  KEY `idx_path` (`path`(191)),
  KEY `idx_relative_path` (`relative_path`(191)),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资源文件上传记录';

-- 若已存在旧版 fs_resource_file（create_time/update_time），可执行以下迁移：
-- ALTER TABLE `fs_resource_file`
--   CHANGE COLUMN `create_time` `gmt_create` datetime DEFAULT NULL COMMENT '创建时间',
--   CHANGE COLUMN `update_time` `gmt_modified` datetime DEFAULT NULL COMMENT '更新时间',
--   ADD COLUMN `is_deleted` tinyint(4) DEFAULT 0 COMMENT '是否删除;0未删;1已删' AFTER `remark`,
--   ADD COLUMN `gmt_delete` datetime DEFAULT NULL COMMENT '删除时间' AFTER `gmt_modified`,
--   ADD COLUMN `created_user_id` bigint(20) DEFAULT NULL COMMENT '创建人' AFTER `gmt_delete`,
--   ADD COLUMN `modified_user_id` bigint(20) DEFAULT NULL COMMENT '更新人' AFTER `created_user_id`,
--   ADD COLUMN `deleted_user_id` bigint(20) DEFAULT NULL COMMENT '删除人' AFTER `modified_user_id`,
--   ADD KEY `idx_is_deleted` (`is_deleted`);

-- ----------------------------
-- 文件管理菜单（前端已挂 constantRoutes，以下供动态菜单/权限同步可选执行）
-- ----------------------------
-- INSERT INTO sys_menu VALUES('2000', '文件管理', '0', '5', 'file', null, '', '', 1, 0, 'M', '0', '0', '', 'upload', 'admin', sysdate(), '', null, '文件管理目录');
-- INSERT INTO sys_menu VALUES('2001', '文件上传', '2000', '1', 'upload', 'file/upload/index', '', '', 1, 0, 'C', '0', '0', 'file:upload:list', 'upload', 'admin', sysdate(), '', null, '普通文件上传');
-- INSERT INTO sys_menu VALUES('2002', '大文件上传', '2000', '2', 'bigUpload', 'file/bigUpload/index', '', '', 1, 0, 'C', '0', '0', 'file:bigUpload:list', 'upload', 'admin', sysdate(), '', null, '大文件分片上传');
-- INSERT INTO sys_role_menu VALUES ('1', '2000');
-- INSERT INTO sys_role_menu VALUES ('1', '2001');
-- INSERT INTO sys_role_menu VALUES ('1', '2002');
