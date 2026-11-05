package com.winter.cloud.common.datasource.ddl;

import com.winter.cloud.common.core.utils.StringUtils;
import com.winter.cloud.common.datasource.ddl.model.DsColumnDef;
import com.winter.cloud.common.datasource.ddl.model.DsIndexRequest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class SqlServerDdlDialect extends AbstractDsDdlDialect {

    private static final Pattern TYPE = Pattern.compile(
            "^(varchar|nvarchar|char|nchar|text|ntext|int|integer|bigint|smallint|tinyint|"
                    + "decimal|numeric|float|real|date|datetime|datetime2|smalldatetime|time|"
                    + "bit|uniqueidentifier|varbinary|binary|image|money|smallmoney)"
                    + "(\\(\\d+(,\\d+)?\\)|\\(max\\))?$",
            Pattern.CASE_INSENSITIVE);

    @Override
    public boolean supports(String datasourceType) {
        String t = datasourceType == null ? "" : datasourceType.toLowerCase(Locale.ROOT);
        return t.contains("sqlserver") || t.contains("mssql");
    }

    @Override
    public String name() { return "sqlserver"; }

    @Override
    protected String dsKey() { return "sqlserver"; }

    @Override
    protected Pattern typePattern() { return TYPE; }

    @Override
    protected String typeHint() { return "示例：nvarchar(64)、bigint、decimal(10,2)、datetime2"; }

    @Override
    protected String mapCurrentTimestamp(String v) {
        return "GETDATE()";
    }

    @Override
    public List<String> supportedIndexMethods() {
        return Arrays.asList("BTREE"); // SQL Server 非聚集默认，界面统一显示 BTREE
    }

    @Override
    public List<String> buildCreateTableSqls(String tableName, String comment, List<DsColumnDef> columns) {
        List<String> body = new ArrayList<>();
        List<String> pkCols = new ArrayList<>();
        for (DsColumnDef col : columns) {
            body.add(buildInlineColumnClause(col, false));
            if (Boolean.TRUE.equals(col.getPrimaryKey())) {
                pkCols.add(quote(col.getName()));
            }
        }
        if (!pkCols.isEmpty()) {
            body.add("PRIMARY KEY (" + String.join(",", pkCols) + ")");
        }
        // SQL Server 表注释需扩展属性，这里忽略 comment 以免依赖存储过程权限
        return Collections.singletonList("CREATE TABLE " + quote(tableName) + " (\n  "
                + String.join(",\n  ", body) + "\n)");
    }

    @Override
    public String buildDropTableSql(String tableName) {
        return "DROP TABLE " + quote(tableName);
    }

    @Override
    public List<String> buildAlterColumnSqls(String tableName, String action, DsColumnDef column) {
        String qTable = quote(tableName);
        switch (action) {
            case "ADD":
                return Collections.singletonList("ALTER TABLE " + qTable + " ADD " + buildInlineColumnClause(column, false));
            case "MODIFY": {
                // SQL Server: ALTER COLUMN 不支持一次改默认值；仅改类型/空值
                String type = normalizeType(column.getType());
                String nullSql = Boolean.FALSE.equals(column.getNullable()) ? " NOT NULL" : " NULL";
                return Collections.singletonList("ALTER TABLE " + qTable + " ALTER COLUMN "
                        + quote(column.getName()) + " " + type + nullSql);
            }
            case "DROP":
                return Collections.singletonList("ALTER TABLE " + qTable + " DROP COLUMN " + quote(column.getName()));
            default:
                throw new IllegalArgumentException("不支持的操作：" + action);
        }
    }

    @Override
    public String buildCreateIndexSql(DsIndexRequest request) {
        if (request.getColumns() == null || request.getColumns().isEmpty()) {
            throw new IllegalArgumentException("索引列不能为空");
        }
        String indexType = normalizeIndexType(request);
        if ("FULLTEXT".equals(indexType) || "SPATIAL".equals(indexType)) {
            throw new IllegalArgumentException("SQL Server 方言暂不支持 FULLTEXT/SPATIAL 快捷创建");
        }
        List<String> cols = new ArrayList<>();
        for (String c : request.getColumns()) {
            cols.add(quote(c));
        }
        String unique = "UNIQUE".equals(indexType) ? "UNIQUE " : "";
        return "CREATE " + unique + "NONCLUSTERED INDEX " + quote(request.getIndexName())
                + " ON " + quote(request.getTableName())
                + " (" + String.join(",", cols) + ")";
    }

    @Override
    public String buildDropIndexSql(String tableName, String indexName) {
        if ("PRIMARY".equalsIgnoreCase(indexName) || indexName.toUpperCase(Locale.ROOT).startsWith("PK_")) {
            throw new IllegalArgumentException("不允许直接删除主键索引：" + indexName);
        }
        return "DROP INDEX " + quote(indexName) + " ON " + quote(tableName);
    }

    @Override
    public List<Map<String, Object>> listIndexes(JdbcTemplate jdbc, String tableName) {
        String sql = "SELECT i.name AS index_name, i.is_unique, i.is_primary_key, i.type_desc, "
                + "STUFF((SELECT ',' + c.name FROM sys.index_columns ic "
                + " INNER JOIN sys.columns c ON ic.object_id = c.object_id AND ic.column_id = c.column_id "
                + " WHERE ic.object_id = i.object_id AND ic.index_id = i.index_id "
                + " ORDER BY ic.key_ordinal FOR XML PATH(''), TYPE).value('.', 'NVARCHAR(MAX)'), 1, 1, '') AS columns "
                + "FROM sys.indexes i "
                + "INNER JOIN sys.tables t ON i.object_id = t.object_id "
                + "WHERE t.name = ? AND i.name IS NOT NULL "
                + "ORDER BY i.name";
        List<Map<String, Object>> raw = jdbc.queryForList(sql, tableName);
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map<String, Object> row : raw) {
            String name = String.valueOf(row.get("index_name"));
            boolean primary = Boolean.TRUE.equals(row.get("is_primary_key")) || Integer.valueOf(1).equals(toInt(row.get("is_primary_key")));
            boolean unique = Boolean.TRUE.equals(row.get("is_unique")) || Integer.valueOf(1).equals(toInt(row.get("is_unique")));
            String indexType = primary ? "PRIMARY" : (unique ? "UNIQUE" : "NORMAL");
            list.add(indexItem(name, indexType, "BTREE", "", splitCsv(row.get("columns")), unique || primary));
        }
        return list;
    }
}
