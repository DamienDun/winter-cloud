package com.winter.cloud.common.datasource.ddl;

import com.winter.cloud.common.core.utils.StringUtils;
import com.winter.cloud.common.datasource.ddl.model.DsColumnDef;
import com.winter.cloud.common.datasource.ddl.model.DsIndexRequest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class PostgresqlDdlDialect extends AbstractDsDdlDialect {

    private static final Pattern TYPE = Pattern.compile(
            "^(varchar|charactervarying|char|character|text|int|integer|int4|bigint|int8|smallint|int2|"
                    + "numeric|decimal|real|float4|doubleprecision|float8|date|timestamp|timestamptz|time|timetz|"
                    + "boolean|bool|bytea|json|jsonb|uuid)"
                    + "(\\(\\d+(,\\d+)?\\))?$",
            Pattern.CASE_INSENSITIVE);

    @Override
    public boolean supports(String datasourceType) {
        String t = datasourceType == null ? "" : datasourceType.toLowerCase(Locale.ROOT);
        return t.contains("postgres") || t.contains("pgsql");
    }

    @Override
    public String name() { return "postgresql"; }

    @Override
    protected String dsKey() { return "postgresql"; }

    @Override
    protected Pattern typePattern() { return TYPE; }

    @Override
    protected String typeHint() { return "示例：varchar(64)、bigint、numeric(10,2)、timestamp、boolean"; }

    @Override
    protected String mapCompactType(String compact) {
        if ("charactervarying".equals(compact) || compact.startsWith("charactervarying(")) {
            return compact.replace("charactervarying", "varchar");
        }
        if ("doubleprecision".equals(compact)) {
            return "double precision";
        }
        if ("float8".equals(compact)) {
            return "double precision";
        }
        if ("int4".equals(compact)) return "integer";
        if ("int8".equals(compact)) return "bigint";
        if ("int2".equals(compact)) return "smallint";
        if ("bool".equals(compact)) return "boolean";
        return compact;
    }

    @Override
    public List<String> supportedIndexMethods() {
        return Arrays.asList("BTREE", "HASH", "GIN", "GIST");
    }

    @Override
    public List<String> buildCreateTableSqls(String tableName, String comment, List<DsColumnDef> columns) {
        List<String> sqls = new ArrayList<>();
        // PG 列定义不带 inline comment
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
        sqls.add("CREATE TABLE " + quote(tableName) + " (\n  " + String.join(",\n  ", body) + "\n)");
        if (StringUtils.isNotEmpty(comment)) {
            sqls.add("COMMENT ON TABLE " + quote(tableName) + " IS " + quoteLiteral(comment));
        }
        for (DsColumnDef col : columns) {
            if (StringUtils.isNotEmpty(col.getComment())) {
                sqls.add("COMMENT ON COLUMN " + quote(tableName) + "." + quote(col.getName())
                        + " IS " + quoteLiteral(col.getComment()));
            }
        }
        return sqls;
    }

    @Override
    public String buildDropTableSql(String tableName) {
        return "DROP TABLE " + quote(tableName);
    }

    @Override
    public List<String> buildAlterColumnSqls(String tableName, String action, DsColumnDef column) {
        String qTable = quote(tableName);
        List<String> sqls = new ArrayList<>();
        switch (action) {
            case "ADD":
                sqls.add("ALTER TABLE " + qTable + " ADD COLUMN " + buildInlineColumnClause(column, false));
                if (StringUtils.isNotEmpty(column.getComment())) {
                    sqls.add("COMMENT ON COLUMN " + qTable + "." + quote(column.getName())
                            + " IS " + quoteLiteral(column.getComment()));
                }
                return sqls;
            case "MODIFY": {
                String qCol = quote(column.getName());
                String type = normalizeType(column.getType());
                sqls.add("ALTER TABLE " + qTable + " ALTER COLUMN " + qCol + " TYPE " + type
                        + " USING " + qCol + "::" + type);
                if (Boolean.FALSE.equals(column.getNullable())) {
                    sqls.add("ALTER TABLE " + qTable + " ALTER COLUMN " + qCol + " SET NOT NULL");
                } else {
                    sqls.add("ALTER TABLE " + qTable + " ALTER COLUMN " + qCol + " DROP NOT NULL");
                }
                if (StringUtils.isNotEmpty(column.getDefaultValue())) {
                    sqls.add("ALTER TABLE " + qTable + " ALTER COLUMN " + qCol + " SET DEFAULT "
                            + normalizeDefault(column.getDefaultValue()));
                } else {
                    sqls.add("ALTER TABLE " + qTable + " ALTER COLUMN " + qCol + " DROP DEFAULT");
                }
                if (column.getComment() != null) {
                    sqls.add("COMMENT ON COLUMN " + qTable + "." + qCol + " IS "
                            + quoteLiteral(column.getComment()));
                }
                return sqls;
            }
            case "DROP":
                sqls.add("ALTER TABLE " + qTable + " DROP COLUMN " + quote(column.getName()));
                return sqls;
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
            throw new IllegalArgumentException("PostgreSQL 请使用 GIN/GIST 方法，不支持 FULLTEXT/SPATIAL 类型名");
        }
        String method = normalizeIndexMethod(indexType, request.getIndexMethod());
        List<String> cols = new ArrayList<>();
        for (String c : request.getColumns()) {
            cols.add(quote(c));
        }
        String unique = "UNIQUE".equals(indexType) ? "UNIQUE " : "";
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE ").append(unique).append("INDEX ").append(quote(request.getIndexName()))
                .append(" ON ").append(quote(request.getTableName()));
        if (StringUtils.isNotEmpty(method)) {
            sb.append(" USING ").append(method);
        }
        sb.append(" (").append(String.join(",", cols)).append(")");
        return sb.toString();
    }

    @Override
    public String buildDropIndexSql(String tableName, String indexName) {
        if ("PRIMARY".equalsIgnoreCase(indexName)) {
            throw new IllegalArgumentException("不允许删除主键约束 PRIMARY");
        }
        return "DROP INDEX " + quote(indexName);
    }

    @Override
    public List<Map<String, Object>> listIndexes(JdbcTemplate jdbc, String tableName) {
        String sql = "SELECT i.relname AS index_name, ix.indisunique AS is_unique, ix.indisprimary AS is_primary, "
                + "am.amname AS index_method, "
                + "array_to_string(ARRAY(SELECT a.attname FROM pg_attribute a "
                + " WHERE a.attrelid = ix.indrelid AND a.attnum = ANY(ix.indkey) ORDER BY a.attnum), ',') AS columns "
                + "FROM pg_class t "
                + "JOIN pg_index ix ON t.oid = ix.indrelid "
                + "JOIN pg_class i ON i.oid = ix.indexrelid "
                + "JOIN pg_am am ON i.relam = am.oid "
                + "WHERE t.relkind = 'r' AND t.relname = ? "
                + "AND t.relnamespace = (SELECT oid FROM pg_namespace WHERE nspname = current_schema()) "
                + "ORDER BY i.relname";
        List<Map<String, Object>> raw = jdbc.queryForList(sql, tableName.toLowerCase(Locale.ROOT));
        // also try original case
        if (raw.isEmpty() && !tableName.equals(tableName.toLowerCase(Locale.ROOT))) {
            raw = jdbc.queryForList(sql, tableName);
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map<String, Object> row : raw) {
            String name = String.valueOf(row.get("index_name"));
            boolean isPrimary = Boolean.TRUE.equals(row.get("is_primary"));
            boolean isUnique = Boolean.TRUE.equals(row.get("is_unique"));
            String method = String.valueOf(row.get("index_method")).toUpperCase(Locale.ROOT);
            String indexType = isPrimary ? "PRIMARY" : (isUnique ? "UNIQUE" : "NORMAL");
            list.add(indexItem(name, indexType, method, "", splitCsv(row.get("columns")), isUnique));
        }
        return list;
    }
}
