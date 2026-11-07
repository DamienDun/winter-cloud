ALTER TABLE ds_job_info
  ADD COLUMN source_table varchar(200) DEFAULT NULL COMMENT '源表（JDBC源）' AFTER datasource_type,
  ADD COLUMN target_datasource_id bigint(20) DEFAULT NULL COMMENT '目标数据源id' AFTER source_table,
  ADD COLUMN target_datasource_name varchar(200) DEFAULT NULL COMMENT '目标数据源名称' AFTER target_datasource_id,
  ADD COLUMN target_table varchar(200) DEFAULT NULL COMMENT '目标数据表' AFTER target_datasource_name,
  ADD COLUMN field_mapping_json mediumtext COMMENT '字段映射JSON' AFTER target_table;

ALTER TABLE ds_job_info ADD INDEX idx_ds_job_target_ds (target_datasource_id);
