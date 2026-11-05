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
 * Presto/Trino 精简 DDL：依赖底层 Connector，仅保证常见建删表与加列；
 * 改列/删列/索引能力因 Connector 而异，不支持时给出明确提示。
 */
public class PrestoDdlDialect extends AbstractDsDdlDialect {

    private static final Pattern TYPE = Pattern.compile(
            "^(varchar|char|string|boolean|tinyint|smallint|integer|int|bigint|real|double|decimal|numeric|"
                    + "date|timestamp|time|varbinary|uuid|json|array|map|row)"
                    + "(\\(\\d+(,\\d+)?\\))?$",
            Pattern.CASE_INSENSITIVE);

    @Override
    public boolean supports(String datasourceType) {
        String t = datasourceType == null ? "" : datasourceType.toLowerCase(Locale.ROOT);
        return t.contains("presto") || t.contains("trino");
    }

    @Override
    public String name() {
        return "presto";
    }

    @Override
    protected String dsKey() {
        return "presto";
    }

    @Override
    protected Pattern typePattern() {
        return TYPE;
    }

    @Override
    protected String typeHint() {
        return "示例：varchar、bigint、decimal(10,2)、timestamp、boolean";
    }

    @Override
    protected String mapCompactType(String compact) {
        if ("int".equals(compact)) {
            return "integer";
        }
        if ("string".equals(compact) || "text".equals(compact)) {
            return "varchar";
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
            StringBuilder sb = new StringBuilder();
            sb.append(quote(col.getName())).append(" ").append(normalizeType(col.getType()));
            // Presto 多数 Connector 忽略列级 NULL/DEFAULT/COMMENT，这里只输出类型
            body.add(sb.toString());
        }
        // comment 在 Presto 无统一语法，忽略
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
                return Collections.singletonList("ALTER TABLE " + qTable + " ADD COLUMN "
                        + quote(column.getName()) + " " + normalizeType(column.getType()));
            case "MODIFY":
                throw new IllegalArgumentException("Presto/Trino 改列能力依赖 Connector，当前统一不支持 MODIFY，请删列后重建");
            case "DROP":
                return Collections.singletonList("ALTER TABLE " + qTable + " DROP COLUMN " + quote(column.getName()));
            default:
                throw new IllegalArgumentException("不支持的操作：" + action);
        }
    }

    @Override
    public String buildCreateIndexSql(DsIndexRequest request) {
        throw new IllegalArgumentException("Presto/Trino 不支持二级索引管理");
    }

    @Override
    public String buildDropIndexSql(String tableName, String indexName) {
        throw new IllegalArgumentException("Presto/Trino 不支持二级索引管理");
    }

    @Override
    public List<Map<String, Object>> listIndexes(JdbcTemplate jdbc, String tableName) {
        return Collections.emptyList();
    }
}
