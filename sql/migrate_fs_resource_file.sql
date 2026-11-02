ALTER TABLE `fs_resource_file`
  CHANGE COLUMN `create_time` `gmt_create` datetime DEFAULT NULL COMMENT '创建时间',
  CHANGE COLUMN `update_time` `gmt_modified` datetime DEFAULT NULL COMMENT '更新时间',
  ADD COLUMN `is_deleted` tinyint(4) DEFAULT 0 COMMENT '是否删除;0未删;1已删' AFTER `remark`,
  ADD COLUMN `gmt_delete` datetime DEFAULT NULL COMMENT '删除时间' AFTER `gmt_modified`,
  ADD COLUMN `created_user_id` bigint(20) DEFAULT NULL COMMENT '创建人' AFTER `gmt_delete`,
  ADD COLUMN `modified_user_id` bigint(20) DEFAULT NULL COMMENT '更新人' AFTER `created_user_id`,
  ADD COLUMN `deleted_user_id` bigint(20) DEFAULT NULL COMMENT '删除人' AFTER `modified_user_id`,
  ADD KEY `idx_is_deleted` (`is_deleted`);