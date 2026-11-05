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

public class OracleDdlDialect extends AbstractDsDdlDialect {

    private static final Pattern TYPE = Pattern.compile(
            "^(varchar2|nvarchar2|varchar|char|nchar|number|integer|int|float|binary_float|binary_double|"
                    + "date|timestamp|clob|nclob|blob|raw|long)"
                    + "(\\(\\d+(,\\d+)?\\))?$",
            Pattern.CASE_INSENSITIVE);

    @Override
    public boolean supports(String datasourceType) {
        String t = datasourceType == null ? "" : datasourceType.toLowerCase(Locale.ROOT);
        return t.contains("oracle");
    }

    @Override
    public String name() { return "oracle"; }

    @Override
    protected String dsKey() { return "oracle"; }

    @Override
    protected Pattern typePattern() { return TYPE; }

    @Override
    protected String typeHint() { return "示例：varchar2(64)、number(19)、date、timestamp、clob"; }

    @Override
    protected String mapCompactType(String compact) {
        if ("varchar".equals(compact) || compact.startsWith("varchar(")) {
            return compact.replace("varchar", "varchar2");
        }
        if ("int".equals(compact) || "integer".equals(compact)) {
            return "number(10)";
        }
        return compact;
    }

    @Override
    protected String mapCurrentTimestamp(String v) {
        return "SYSDATE";
    }

    @Override
    public List<String> supportedIndexMethods() {
        return Collections.singletonList("BTREE");
    }

    @Override
    public List<String> buildCreateTableSqls(String tableName, String comment, List<DsColumnDef> columns) {
        List<String> sqls = new ArrayList<>();
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
                sqls.add("ALTER TABLE " + qTable + " ADD (" + buildInlineColumnClause(column, false) + ")");
                if (StringUtils.isNotEmpty(column.getComment())) {
                    sqls.add("COMMENT ON COLUMN " + qTable + "." + quote(column.getName())
                            + " IS " + quoteLiteral(column.getComment()));
                }
                return sqls;
            case "MODIFY":
                sqls.add("ALTER TABLE " + qTable + " MODIFY (" + buildInlineColumnClause(column, false) + ")");
                if (column.getComment() != null) {
                    sqls.add("COMMENT ON COLUMN " + qTable + "." + quote(column.getName())
                            + " IS " + quoteLiteral(column.getComment()));
                }
                return sqls;
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
            throw new IllegalArgumentException("Oracle 方言暂不支持 FULLTEXT/SPATIAL");
        }
        List<String> cols = new ArrayList<>();
        for (String c : request.getColumns()) {
            cols.add(quote(c));
        }
        String unique = "UNIQUE".equals(indexType) ? "UNIQUE " : "";
        return "CREATE " + unique + "INDEX " + quote(request.getIndexName())
                + " ON " + quote(request.getTableName())
                + " (" + String.join(",", cols) + ")";
    }

    @Override
    public String buildDropIndexSql(String tableName, String indexName) {
        if ("PRIMARY".equalsIgnoreCase(indexName)) {
            throw new IllegalArgumentException("不允许删除主键索引 PRIMARY");
        }
        return "DROP INDEX " + quote(indexName);
    }

    @Override
    public List<Map<String, Object>> listIndexes(JdbcTemplate jdbc, String tableName) {
        String upper = tableName.toUpperCase(Locale.ROOT);
        String sql = "SELECT i.INDEX_NAME AS index_name, i.UNIQUENESS AS uniqueness, i.INDEX_TYPE AS index_type, "
                + "LISTAGG(c.COLUMN_NAME, ',') WITHIN GROUP (ORDER BY c.COLUMN_POSITION) AS columns "
                + "FROM USER_INDEXES i "
                + "JOIN USER_IND_COLUMNS c ON i.INDEX_NAME = c.INDEX_NAME "
                + "WHERE i.TABLE_NAME = ? "
                + "GROUP BY i.INDEX_NAME, i.UNIQUENESS, i.INDEX_TYPE "
                + "ORDER BY i.INDEX_NAME";
        List<Map<String, Object>> raw;
        try {
            raw = jdbc.queryForList(sql, upper);
        } catch (Exception e) {
            // 兼容无 LISTAGG 的老版本：回退简单查询再聚合
            String sql2 = "SELECT i.INDEX_NAME AS index_name, i.UNIQUENESS AS uniqueness, i.INDEX_TYPE AS index_type, "
                    + "c.COLUMN_NAME AS column_name, c.COLUMN_POSITION AS pos "
                    + "FROM USER_INDEXES i JOIN USER_IND_COLUMNS c ON i.INDEX_NAME = c.INDEX_NAME "
                    + "WHERE i.TABLE_NAME = ? ORDER BY i.INDEX_NAME, c.COLUMN_POSITION";
            List<Map<String, Object>> rows = jdbc.queryForList(sql2, upper);
            Map<String, Map<String, Object>> grouped = new java.util.LinkedHashMap<>();
            for (Map<String, Object> row : rows) {
                String name = String.valueOf(row.get("index_name"));
                Map<String, Object> g = grouped.get(name);
                if (g == null) {
                    g = new java.util.LinkedHashMap<>();
                    g.put("index_name", name);
                    g.put("uniqueness", row.get("uniqueness"));
                    g.put("index_type", row.get("index_type"));
                    g.put("columns", new ArrayList<String>());
                    grouped.put(name, g);
                }
                @SuppressWarnings("unchecked")
                List<String> cols = (List<String>) g.get("columns");
                cols.add(String.valueOf(row.get("column_name")));
            }
            List<Map<String, Object>> list = new ArrayList<>();
            for (Map<String, Object> g : grouped.values()) {
                @SuppressWarnings("unchecked")
                List<String> cols = (List<String>) g.get("columns");
                boolean unique = "UNIQUE".equalsIgnoreCase(String.valueOf(g.get("uniqueness")));
                list.add(indexItem(String.valueOf(g.get("index_name")), unique ? "UNIQUE" : "NORMAL",
                        "BTREE", "", cols, unique));
            }
            return list;
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map<String, Object> row : raw) {
            boolean unique = "UNIQUE".equalsIgnoreCase(String.valueOf(row.get("uniqueness")));
            list.add(indexItem(String.valueOf(row.get("index_name")), unique ? "UNIQUE" : "NORMAL",
                    "BTREE", "", splitCsv(row.get("columns")), unique));
        }
        return list;
    }
}
