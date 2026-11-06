-- API数据源表增量：增加分组字段 + 字段注释中文化
-- 适用：库中已存在 ds_api_datasource 时执行本脚本

ALTER TABLE ds_api_datasource
  ADD COLUMN datasource_group varchar(200) DEFAULT 'Default' COMMENT '数据源分组' AFTER datasource_name;

ALTER TABLE ds_api_datasource
  ADD INDEX idx_ds_api_group (datasource_group);

ALTER TABLE ds_api_datasource
  MODIFY COLUMN id bigint(20) NOT NULL COMMENT '主键',
  MODIFY COLUMN datasource_name varchar(100) NOT NULL COMMENT '数据源名称',
  MODIFY COLUMN pi_url varchar(512) NOT NULL COMMENT '接口地址',
  MODIFY COLUMN 
eq_type tinyint(4) NOT NULL DEFAULT 1 COMMENT '请求方式：0 POST，1 GET',
  MODIFY COLUMN data_item_key varchar(512) DEFAULT NULL COMMENT '数据项KEY（响应取值路径）',
  MODIFY COLUMN 
eq_params_json mediumtext COMMENT '请求参数JSON',
  MODIFY COLUMN 
ely_datasource_id bigint(20) DEFAULT NULL COMMENT '依赖API数据源ID',
  MODIFY COLUMN elong_to_rely_datasource tinyint(4) NOT NULL DEFAULT 1 COMMENT '是否依赖数据源：0是，1否',
  MODIFY COLUMN datasource_desc varchar(200) DEFAULT NULL COMMENT '数据源描述',
  MODIFY COLUMN status tinyint(1) NOT NULL DEFAULT 1 COMMENT '状态：0停用，1启用',
  MODIFY COLUMN 
emark varchar(500) DEFAULT NULL COMMENT '备注',
  MODIFY COLUMN is_deleted tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否，1是',
  MODIFY COLUMN gmt_create datetime DEFAULT NULL COMMENT '创建时间',
  MODIFY COLUMN gmt_modified datetime DEFAULT NULL COMMENT '修改时间',
  MODIFY COLUMN gmt_delete datetime DEFAULT NULL COMMENT '删除时间',
  MODIFY COLUMN created_user_id bigint(20) DEFAULT NULL COMMENT '创建人',
  MODIFY COLUMN modified_user_id bigint(20) DEFAULT NULL COMMENT '更新人',
  MODIFY COLUMN deleted_user_id bigint(20) DEFAULT NULL COMMENT '删除人',
  COMMENT='API数据源配置';
