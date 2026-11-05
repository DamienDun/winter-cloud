package com.winter.cloud.common.datasource.ddl;

import com.winter.cloud.common.datasource.ddl.model.DsColumnDef;
import com.winter.cloud.common.datasource.ddl.model.DsIndexRequest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

/**
 * 表结构 DDL 方言
 */
public interface DsDdlDialect {

    boolean supports(String datasourceType);

    String name();

    List<String> supportedIndexTypes();

    List<String> supportedIndexMethods();

    boolean supportsIndexComment();

    List<String> buildCreateTableSqls(String tableName, String comment, List<DsColumnDef> columns);

    String buildDropTableSql(String tableName);

    List<String> buildAlterColumnSqls(String tableName, String action, DsColumnDef column);

    String buildCreateIndexSql(DsIndexRequest request);

    String buildDropIndexSql(String tableName, String indexName);

    List<Map<String, Object>> listIndexes(JdbcTemplate jdbc, String tableName);
}
