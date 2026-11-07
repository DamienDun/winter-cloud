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

public class MysqlDdlDialect extends AbstractDsDdlDialect {

    private static final Pattern TYPE = Pattern.compile(
            "^(varchar|char|text|tinytext|mediumtext|longtext|int|integer|bigint|smallint|tinyint|"
                    + "decimal|numeric|float|double|date|datetime|timestamp|time|bit|boolean|bool|blob|json)"
                    + "(\\(\\d+(,\\d+)?\\))?$",
            Pattern.CASE_INSENSITIVE);

    @Override
    public boolean supports(String datasourceType) {
        String t = datasourceType == null ? "" : datasourceType.toLowerCase(Locale.ROOT);
        return t.contains("mysql") || t.contains("mariadb");
    }

    @Override
    public String name() { return "mysql"; }

    @Override
    protected Pattern typePattern() { return TYPE; }

    @Override
    protected String typeHint() { return "示例：varchar(64)、bigint、decimal(10,2)、datetime"; }

    @Override
    public List<String> supportedIndexTypes() {
        return Arrays.asList("NORMAL", "UNIQUE", "FULLTEXT", "SPATIAL");
    }

    @Override
    public List<String> supportedIndexMethods() {
        return Arrays.asList("BTREE", "HASH");
    }

    @Override
    public boolean supportsIndexComment() { return true; }

    @Override
    public List<String> buildCreateTableSqls(String tableName, String comment, List<DsColumnDef> columns) {
        List<String> parts = buildPkColumnClauses(columns);
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE ").append(quote(tableName)).append(" (\n  ");
        sb.append(String.join(",\n  ", parts));
        sb.append("\n) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        if (StringUtils.isNotEmpty(comment)) {
            sb.append(" COMMENT=").append(quoteLiteral(comment));
        }
        return Collections.singletonList(sb.toString());
    }

    @Override
    public String buildDropTableSql(String tableName) {
        return "DROP TABLE " + quote(tableName);
    }

    @Override
    public List<String> buildAlterColumnSqls(String tableName, String action, DsColumnDef column) {
        String qTable = quote(tableName);
        switch (action) {
            case "ADD": {
                String add = "ALTER TABLE " + qTable + " ADD COLUMN " + buildInlineColumnClause(column, true);
                // 与源表顺序对齐：插到指定列之后
                if (StringUtils.isNotEmpty(column.getAfterColumn())) {
                    add = add + " AFTER " + quote(column.getAfterColumn());
                }
                return Collections.singletonList(add);
            }
            case "MODIFY":
                return Collections.singletonList("ALTER TABLE " + qTable + " MODIFY COLUMN " + buildInlineColumnClause(column, true));
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
        if ("PRIMARY".equalsIgnoreCase(request.getIndexName())) {
            throw new IllegalArgumentException("请使用表主键定义 PRIMARY");
        }
        String indexType = normalizeIndexType(request);
        String indexMethod = normalizeIndexMethod(indexType, request.getIndexMethod());
        List<String> cols = new ArrayList<>();
        for (String c : request.getColumns()) {
            cols.add(quote(c));
        }
        String kind;
        switch (indexType) {
            case "UNIQUE": kind = "UNIQUE INDEX"; break;
            case "FULLTEXT": kind = "FULLTEXT INDEX"; break;
            case "SPATIAL": kind = "SPATIAL INDEX"; break;
            default: kind = "INDEX";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE ").append(kind).append(" ").append(quote(request.getIndexName()))
                .append(" ON ").append(quote(request.getTableName()))
                .append(" (").append(String.join(",", cols)).append(")");
        if (("NORMAL".equals(indexType) || "UNIQUE".equals(indexType)) && StringUtils.isNotEmpty(indexMethod)) {
            sb.append(" USING ").append(indexMethod);
        }
        if (StringUtils.isNotEmpty(request.getComment())) {
            sb.append(" COMMENT ").append(quoteLiteral(request.getComment()));
        }
        return sb.toString();
    }

    @Override
    public String buildDropIndexSql(String tableName, String indexName) {
        if ("PRIMARY".equalsIgnoreCase(indexName)) {
            throw new IllegalArgumentException("不允许删除主键索引 PRIMARY");
        }
        return "DROP INDEX " + quote(indexName) + " ON " + quote(tableName);
    }

    @Override
    public List<Map<String, Object>> listIndexes(JdbcTemplate jdbc, String tableName) {
        String sql = "SELECT INDEX_NAME AS indexName, NON_UNIQUE AS nonUnique, INDEX_TYPE AS indexTypeRaw, "
                + "IFNULL(INDEX_COMMENT,'') AS indexComment, "
                + "GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ',') AS columnNames "
                + "FROM information_schema.STATISTICS "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? "
                + "GROUP BY INDEX_NAME, NON_UNIQUE, INDEX_TYPE, INDEX_COMMENT "
                + "ORDER BY INDEX_NAME";
        List<Map<String, Object>> raw = jdbc.queryForList(sql, tableName);
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map<String, Object> row : raw) {
            String name = String.valueOf(row.get("indexName"));
            int nonUnique = toInt(row.get("nonUnique")) == null ? 1 : toInt(row.get("nonUnique"));
            String typeRaw = String.valueOf(row.get("indexTypeRaw"));
            String indexType;
            String indexMethod;
            if ("PRIMARY".equalsIgnoreCase(name)) {
                indexType = "PRIMARY";
                indexMethod = typeRaw;
            } else if ("FULLTEXT".equalsIgnoreCase(typeRaw)) {
                indexType = "FULLTEXT";
                indexMethod = "";
            } else if ("SPATIAL".equalsIgnoreCase(typeRaw)) {
                indexType = "SPATIAL";
                indexMethod = "";
            } else if (nonUnique == 0) {
                indexType = "UNIQUE";
                indexMethod = typeRaw;
            } else {
                indexType = "NORMAL";
                indexMethod = typeRaw;
            }
            list.add(indexItem(name, indexType, indexMethod, String.valueOf(row.get("indexComment")),
                    splitCsv(row.get("columnNames")), nonUnique == 0));
        }
        return list;
    }
}
