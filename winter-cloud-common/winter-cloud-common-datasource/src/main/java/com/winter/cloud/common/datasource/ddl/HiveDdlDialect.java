package com.winter.cloud.common.datasource.ddl;

import com.winter.cloud.common.core.utils.StringUtils;
import com.winter.cloud.common.datasource.ddl.model.DsColumnDef;
import com.winter.cloud.common.datasource.ddl.model.DsIndexRequest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Hive 精简 DDL：建删表、增删改列；传统二级索引不支持
 */
public class HiveDdlDialect extends AbstractDsDdlDialect {

    private static final Pattern TYPE = Pattern.compile(
            "^(string|varchar|char|boolean|tinyint|smallint|int|integer|bigint|float|double|decimal|numeric|"
                    + "date|timestamp|binary|array|map|struct)"
                    + "(\\(\\d+(,\\d+)?\\))?$",
            Pattern.CASE_INSENSITIVE);

    @Override
    public boolean supports(String datasourceType) {
        String t = datasourceType == null ? "" : datasourceType.toLowerCase(Locale.ROOT);
        return t.contains("hive");
    }

    @Override
    public String name() {
        return "hive";
    }

    @Override
    protected String dsKey() {
        return "hive";
    }

    @Override
    protected Pattern typePattern() {
        return TYPE;
    }

    @Override
    protected String typeHint() {
        return "示例：string、bigint、varchar(64)、decimal(10,2)、timestamp";
    }

    @Override
    protected String mapCompactType(String compact) {
        if ("integer".equals(compact)) {
            return "int";
        }
        if (compact.startsWith("varchar") || compact.startsWith("char") || compact.equals("text")) {
            return compact.equals("text") ? "string" : compact;
        }
        return compact;
    }

    @Override
    public List<String> supportedIndexTypes() {
        return Collections.emptyList();
    }

    @Override
    public List<String> supportedIndexMethods() {
        return Collections.emptyList();
    }

    @Override
    public List<String> buildCreateTableSqls(String tableName, String comment, List<DsColumnDef> columns) {
        List<String> body = new ArrayList<>();
        for (DsColumnDef col : columns) {
            // Hive 建表一般不强调 PRIMARY KEY（非强制约束），仅作列定义
            StringBuilder sb = new StringBuilder();
            sb.append(quote(col.getName())).append(" ").append(normalizeType(col.getType()));
            if (StringUtils.isNotEmpty(col.getComment())) {
                sb.append(" COMMENT ").append(quoteLiteral(col.getComment()));
            }
            body.add(sb.toString());
        }
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE ").append(quote(tableName)).append(" (\n  ");
        sql.append(String.join(",\n  ", body));
        sql.append("\n)");
        if (StringUtils.isNotEmpty(comment)) {
            sql.append(" COMMENT ").append(quoteLiteral(comment));
        }
        sql.append(" STORED AS PARQUET");
        return Collections.singletonList(sql.toString());
    }

    @Override
    public String buildDropTableSql(String tableName) {
        return "DROP TABLE IF EXISTS " + quote(tableName);
    }

    @Override
    public List<String> buildAlterColumnSqls(String tableName, String action, DsColumnDef column) {
        String qTable = quote(tableName);
        switch (action) {
            case "ADD": {
                StringBuilder sb = new StringBuilder();
                sb.append("ALTER TABLE ").append(qTable).append(" ADD COLUMNS (")
                        .append(quote(column.getName())).append(" ").append(normalizeType(column.getType()));
                if (StringUtils.isNotEmpty(column.getComment())) {
                    sb.append(" COMMENT ").append(quoteLiteral(column.getComment()));
                }
                sb.append(")");
                return Collections.singletonList(sb.toString());
            }
            case "MODIFY":
                // Hive：CHANGE COLUMN old new type
                return Collections.singletonList("ALTER TABLE " + qTable + " CHANGE COLUMN "
                        + quote(column.getName()) + " " + quote(column.getName()) + " "
                        + normalizeType(column.getType())
                        + (StringUtils.isNotEmpty(column.getComment())
                        ? " COMMENT " + quoteLiteral(column.getComment()) : ""));
            case "DROP":
                return Collections.singletonList("ALTER TABLE " + qTable + " DROP COLUMN " + quote(column.getName()));
            default:
                throw new IllegalArgumentException("不支持的操作：" + action);
        }
    }

    @Override
    public String buildCreateIndexSql(DsIndexRequest request) {
        throw new IllegalArgumentException("Hive 不支持传统二级索引管理（可用分区/分桶替代）");
    }

    @Override
    public String buildDropIndexSql(String tableName, String indexName) {
        throw new IllegalArgumentException("Hive 不支持传统二级索引管理");
    }

    @Override
    public List<Map<String, Object>> listIndexes(JdbcTemplate jdbc, String tableName) {
        return Collections.emptyList();
    }
}
