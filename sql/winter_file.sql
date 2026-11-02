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
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_file_md5` (`file_md5`),
  KEY `idx_path` (`path`(191)),
  KEY `idx_relative_path` (`relative_path`(191))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资源文件上传记录';