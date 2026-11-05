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

/**
 * ClickHouse 精简 DDL：MergeTree 建删表、改列；跳数索引（minmax/set）
 */
public class ClickHouseDdlDialect extends AbstractDsDdlDialect {

    private static final Pattern TYPE = Pattern.compile(
            "^(string|fixedstring|varchar|char|int8|int16|int32|int64|uint8|uint16|uint32|uint64|"
                    + "float32|float64|decimal|decimal32|decimal64|decimal128|bool|boolean|"
                    + "date|datetime|datetime64|uuid|nullable)"
                    + "(\\(\\d+(,\\d+)?\\))?$",
            Pattern.CASE_INSENSITIVE);

    @Override
    public boolean supports(String datasourceType) {
        String t = datasourceType == null ? "" : datasourceType.toLowerCase(Locale.ROOT);
        return t.contains("clickhouse");
    }

    @Override
    public String name() {
        return "clickhouse";
    }

    @Override
    protected String dsKey() {
        return "clickhouse";
    }

    @Override
    protected Pattern typePattern() {
        return TYPE;
    }

    @Override
    protected String typeHint() {
        return "示例：String、Int64、UInt32、Decimal(10,2)、DateTime；也可用 varchar/bigint 自动映射";
    }

    @Override
    protected String mapCompactType(String compact) {
        if ("varchar".equals(compact) || compact.startsWith("varchar(") || "string".equals(compact) || "text".equals(compact)) {
            return "String";
        }
        if ("bigint".equals(compact) || "int64".equals(compact)) {
            return "Int64";
        }
        if ("int".equals(compact) || "integer".equals(compact) || "int32".equals(compact)) {
            return "Int32";
        }
        if ("smallint".equals(compact) || "int16".equals(compact)) {
            return "Int16";
        }
        if ("tinyint".equals(compact) || "int8".equals(compact)) {
            return "Int8";
        }
        if ("float".equals(compact) || "float32".equals(compact)) {
            return "Float32";
        }
        if ("double".equals(compact) || "float64".equals(compact)) {
            return "Float64";
        }
        if (compact.startsWith("decimal")) {
            // decimal(10,2) -> Decimal(10, 2)
            return compact.replace("decimal", "Decimal").replace(",", ", ");
        }
        if ("datetime".equals(compact)) {
            return "DateTime";
        }
        if ("date".equals(compact)) {
            return "Date";
        }
        if ("boolean".equals(compact) || "bool".equals(compact) || "bit".equals(compact)) {
            return "UInt8";
        }
        if ("uuid".equals(compact)) {
            return "UUID";
        }
        // ClickHouse 类型大小写敏感习惯：首字母大写常见族
        if (compact.startsWith("nullable(")) {
            return "Nullable(" + mapCompactType(compact.substring(9, compact.length() - 1)) + ")";
        }
        return Character.toUpperCase(compact.charAt(0)) + compact.substring(1);
    }

    @Override
    public List<String> supportedIndexTypes() {
        // 复用 NORMAL 表示跳数索引
        return Collections.singletonList("NORMAL");
    }

    @Override
    public List<String> supportedIndexMethods() {
        return Arrays.asList("minmax", "set", "ngrambf_v1", "tokenbf_v1", "bloom_filter");
    }

    @Override
    public List<String> buildCreateTableSqls(String tableName, String comment, List<DsColumnDef> columns) {
        List<String> body = new ArrayList<>();
        List<String> orderCols = new ArrayList<>();
        for (DsColumnDef col : columns) {
            String type = normalizeType(col.getType());
            // 默认可空列包一层 Nullable（主键/排序键除外）
            if (!Boolean.FALSE.equals(col.getNullable()) && !Boolean.TRUE.equals(col.getPrimaryKey())
                    && !type.startsWith("Nullable(")) {
                type = "Nullable(" + type + ")";
            }
            StringBuilder sb = new StringBuilder();
            sb.append(quote(col.getName())).append(" ").append(type);
            if (StringUtils.isNotEmpty(col.getComment())) {
                sb.append(" COMMENT ").append(quoteLiteral(col.getComment()));
            }
            body.add(sb.toString());
            if (Boolean.TRUE.equals(col.getPrimaryKey())) {
                orderCols.add(quote(col.getName()));
            }
        }
        String orderBy = orderCols.isEmpty() ? "tuple()" : String.join(", ", orderCols);
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE ").append(quote(tableName)).append(" (\n  ");
        sql.append(String.join(",\n  ", body));
        sql.append("\n) ENGINE = MergeTree\nORDER BY (").append(orderBy).append(")");
        if (StringUtils.isNotEmpty(comment)) {
            sql.append("\nCOMMENT ").append(quoteLiteral(comment));
        }
        return Collections.singletonList(sql.toString());
    }

    @Override
    public String buildDropTableSql(String tableName) {
        return "DROP TABLE IF EXISTS " + quote(tableName);
    }

    @Override
    public List<String> buildAlterColumnSqls(String tableName, String action, DsColumnDef column) {
        String qTable = quote(tableName);
        String type = normalizeType(column.getType());
        if (!Boolean.FALSE.equals(column.getNullable()) && !type.startsWith("Nullable(")) {
            type = "Nullable(" + type + ")";
        }
        switch (action) {
            case "ADD": {
                StringBuilder sb = new StringBuilder();
                sb.append("ALTER TABLE ").append(qTable).append(" ADD COLUMN ")
                        .append(quote(column.getName())).append(" ").append(type);
                if (StringUtils.isNotEmpty(column.getComment())) {
                    sb.append(" COMMENT ").append(quoteLiteral(column.getComment()));
                }
                return Collections.singletonList(sb.toString());
            }
            case "MODIFY": {
                StringBuilder sb = new StringBuilder();
                sb.append("ALTER TABLE ").append(qTable).append(" MODIFY COLUMN ")
                        .append(quote(column.getName())).append(" ").append(type);
                if (StringUtils.isNotEmpty(column.getComment())) {
                    sb.append(" COMMENT ").append(quoteLiteral(column.getComment()));
                }
                return Collections.singletonList(sb.toString());
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
        // ClickHouse 跳数索引一次通常一个表达式/列
        String col = quote(request.getColumns().get(0));
        String method = StringUtils.isEmpty(request.getIndexMethod()) ? "minmax" : request.getIndexMethod().trim();
        if (!supportedIndexMethods().contains(method)) {
            throw new IllegalArgumentException("ClickHouse 跳数索引类型不支持：" + method
                    + "（支持：" + String.join("/", supportedIndexMethods()) + "）");
        }
        return "ALTER TABLE " + quote(request.getTableName())
                + " ADD INDEX " + quote(request.getIndexName()) + " " + col
                + " TYPE " + method + " GRANULARITY 4";
    }

    @Override
    public String buildDropIndexSql(String tableName, String indexName) {
        return "ALTER TABLE " + quote(tableName) + " DROP INDEX " + quote(indexName);
    }

    @Override
    public List<Map<String, Object>> listIndexes(JdbcTemplate jdbc, String tableName) {
        try {
            String sql = "SELECT name AS index_name, type AS index_type, expr AS expr, granularity AS granularity "
                    + "FROM system.data_skipping_indices WHERE database = currentDatabase() AND table = ?";
            List<Map<String, Object>> raw = jdbc.queryForList(sql, tableName);
            List<Map<String, Object>> list = new ArrayList<>();
            for (Map<String, Object> row : raw) {
                String expr = row.get("expr") == null ? "" : String.valueOf(row.get("expr"));
                list.add(indexItem(String.valueOf(row.get("index_name")), "NORMAL",
                        String.valueOf(row.get("index_type")), "",
                        Collections.singletonList(expr), false));
            }
            return list;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
