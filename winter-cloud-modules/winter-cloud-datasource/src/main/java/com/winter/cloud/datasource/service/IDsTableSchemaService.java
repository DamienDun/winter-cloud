package com.winter.cloud.datasource.service;

import com.winter.cloud.common.datasource.ddl.model.DsIndexRequest;
import com.winter.cloud.datasource.domain.dto.DsAlterColumnRequest;
import com.winter.cloud.datasource.domain.dto.DsCreateTableRequest;
import com.winter.cloud.datasource.domain.dto.DsDropTableRequest;

import java.util.List;
import java.util.Map;

/**
 * 远程库表结构 DDL（建表/删表/改字段/索引）
 */
public interface IDsTableSchemaService {

    void createTable(DsCreateTableRequest request);

    void dropTable(DsDropTableRequest request);

    void alterColumn(DsAlterColumnRequest request);

    void createIndex(DsIndexRequest request);

    void alterIndex(DsIndexRequest request);

    void dropIndex(DsIndexRequest request);

    List<Map<String, Object>> listIndexes(Long datasourceId, String tableName);

    Map<String, Object> capabilities(Long datasourceId);
}