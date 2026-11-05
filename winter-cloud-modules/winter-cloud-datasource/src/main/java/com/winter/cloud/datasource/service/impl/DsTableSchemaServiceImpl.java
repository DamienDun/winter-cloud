package com.winter.cloud.datasource.service.impl;

import com.winter.cloud.common.core.utils.StringUtils;
import com.winter.cloud.common.datasource.query.AbstractDsQueryTool;
import com.winter.cloud.datasource.domain.dto.*;
import com.winter.cloud.datasource.domain.entity.DsJdbcDatasource;
import com.winter.cloud.datasource.mapper.DsJdbcDatasourceMapper;
import com.winter.cloud.datasource.service.IDsTableSchemaService;
import com.winter.cloud.datasource.support.DsQueryToolSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 表结构 DDL（当前以 MySQL 语法为主，其它库会明确提示不支持）
 */
@Service
public class DsTableSchemaServiceImpl implements IDsTableSchemaService {

    private static final Pattern TYPE_PATTERN = Pattern.compile(
            "^(varchar|char|text|tinytext|mediumtext|longtext|int|integer|bigint|smallint|tinyint|"
                    + "decimal|numeric|float|double|date|datetime|timestamp|time|bit|boolean|bool|blob|json)"
                    + "(\\(\\d+(,\\d+)?\\))?$",
            Pattern.CASE_INSENSITIVE);

    private static final Set<String> MYSQL_TYPES = new HashSet<>(Arrays.asList(
            "mysql", "mariadb"
    ));

    @Autowired
    private DsQueryToolSupport queryToolSupport;

    @Autowired
    private DsJdbcDatasourceMapper datasourceMapper;

    @Override
    public void createTable(DsCreateTableRequest request) {
        DsJdbcDatasource ds = requireMysql(request.getDatasourceId());
        AbstractDsQueryTool tool = queryToolSupport.open(request.getDatasourceId());
        try {
            String table = request.getTableName().trim();
            DsQueryToolSupport.quoteIdent(ds.getDatasourceType(), table);
            List<String> names = tool.getTableNames();
            boolean exists = names.stream().anyMatch(table::equalsIgnoreCase);
            if (exists) {
                throw new IllegalArgumentException("表已存在：" + table);
            }
            if (request.getColumns() == null || request.getColumns().isEmpty()) {
                throw new IllegalArgumentException("至少需要一列");
            }
            String sql = buildCreateTableSql(ds.getDatasourceType(), table, request.getComment(), request.getColumns());
            tool.getJdbcTemplate().execute(sql);
        } finally {
            queryToolSupport.close(tool);
        }
    }

    @Override
    public void dropTable(DsDropTableRequest request) {
        if (!request.getTableName().equals(request.getConfirmTableName())) {
            throw new IllegalArgumentException("二次确认表名不一致，已取消删除");
        }
        DsJdbcDatasource ds = requireMysql(request.getDatasourceId());
        AbstractDsQueryTool tool = queryToolSupport.open(request.getDatasourceId());
        try {
            assertTableExists(tool, request.getTableName());
            String qTable = DsQueryToolSupport.quoteIdent(ds.getDatasourceType(), request.getTableName());
            tool.getJdbcTemplate().execute("DROP TABLE " + qTable);
        } finally {
            queryToolSupport.close(tool);
        }
    }

    @Override
    public void alterColumn(DsAlterColumnRequest request) {
        DsJdbcDatasource ds = requireMysql(request.getDatasourceId());
        String action = request.getAction() == null ? "" : request.getAction().trim().toUpperCase(Locale.ROOT);
        AbstractDsQueryTool tool = queryToolSupport.open(request.getDatasourceId());
        try {
            assertTableExists(tool, request.getTableName());
            String qTable = DsQueryToolSupport.quoteIdent(ds.getDatasourceType(), request.getTableName());
            DsColumnDef col = request.getColumn();
            if (col == null || StringUtils.isEmpty(col.getName())) {
                throw new IllegalArgumentException("列名不能为空");
            }
            String sql;
            switch (action) {
                case "ADD":
                    sql = "ALTER TABLE " + qTable + " ADD COLUMN " + buildColumnClause(ds.getDatasourceType(), col);
                    break;
                case "MODIFY":
                    sql = "ALTER TABLE " + qTable + " MODIFY COLUMN " + buildColumnClause(ds.getDatasourceType(), col);
                    break;
                case "DROP":
                    String qCol = DsQueryToolSupport.quoteIdent(ds.getDatasourceType(), col.getName());
                    sql = "ALTER TABLE " + qTable + " DROP COLUMN " + qCol;
                    break;
                default:
                    throw new IllegalArgumentException("不支持的操作：" + request.getAction() + "（仅 ADD/MODIFY/DROP）");
            }
            tool.getJdbcTemplate().execute(sql);
        } finally {
            queryToolSupport.close(tool);
        }
    }

    @Override
    public void createIndex(DsIndexRequest request) {
        DsJdbcDatasource ds = requireMysql(request.getDatasourceId());
        AbstractDsQueryTool tool = queryToolSupport.open(request.getDatasourceId());
        try {
            assertTableExists(tool, request.getTableName());
            String sql = buildCreateIndexSql(ds.getDatasourceType(), request);
            tool.getJdbcTemplate().execute(sql);
        } finally {
            queryToolSupport.close(tool);
        }
    }

    @Override
    public void alterIndex(DsIndexRequest request) {
        // MySQL 无直接改索引列/类型的通用语法：先删后建
        if (StringUtils.isEmpty(request.getOldIndexName())) {
            throw new IllegalArgumentException("修改索引时原索引名不能为空");
        }
        if ("PRIMARY".equalsIgnoreCase(request.getOldIndexName())) {
            throw new IllegalArgumentException("不允许修改主键索引 PRIMARY");
        }
        DsJdbcDatasource ds = requireMysql(request.getDatasourceId());
        AbstractDsQueryTool tool = queryToolSupport.open(request.getDatasourceId());
        try {
            assertTableExists(tool, request.getTableName());
            String qTable = DsQueryToolSupport.quoteIdent(ds.getDatasourceType(), request.getTableName());
            String qOld = DsQueryToolSupport.quoteIdent(ds.getDatasourceType(), request.getOldIndexName());
            tool.getJdbcTemplate().execute("DROP INDEX " + qOld + " ON " + qTable);
            tool.getJdbcTemplate().execute(buildCreateIndexSql(ds.getDatasourceType(), request));
        } finally {
            queryToolSupport.close(tool);
        }
    }

    @Override
    public void dropIndex(DsIndexRequest request) {
        DsJdbcDatasource ds = requireMysql(request.getDatasourceId());
        if ("PRIMARY".equalsIgnoreCase(request.getIndexName())) {
            throw new IllegalArgumentException("不允许删除主键索引 PRIMARY");
        }
        AbstractDsQueryTool tool = queryToolSupport.open(request.getDatasourceId());
        try {
            assertTableExists(tool, request.getTableName());
            String qTable = DsQueryToolSupport.quoteIdent(ds.getDatasourceType(), request.getTableName());
            String qIndex = DsQueryToolSupport.quoteIdent(ds.getDatasourceType(), request.getIndexName());
            tool.getJdbcTemplate().execute("DROP INDEX " + qIndex + " ON " + qTable);
        } finally {
            queryToolSupport.close(tool);
        }
    }

    @Override
    public List<Map<String, Object>> listIndexes(Long datasourceId, String tableName) {
        DsJdbcDatasource ds = requireMysql(datasourceId);
        AbstractDsQueryTool tool = queryToolSupport.open(datasourceId);
        try {
            assertTableExists(tool, tableName);
            // 参数化表名：标识符已校验，库名用 DATABASE()
            DsQueryToolSupport.quoteIdent(ds.getDatasourceType(), tableName);
            String sql = "SELECT INDEX_NAME AS indexName, NON_UNIQUE AS nonUnique, INDEX_TYPE AS indexTypeRaw, "
                    + "IFNULL(INDEX_COMMENT,'') AS indexComment, "
                    + "GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ',') AS columnNames "
                    + "FROM information_schema.STATISTICS "
                    + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? "
                    + "GROUP BY INDEX_NAME, NON_UNIQUE, INDEX_TYPE, INDEX_COMMENT "
                    + "ORDER BY INDEX_NAME";
            List<Map<String, Object>> raw = tool.getJdbcTemplate().queryForList(sql, tableName);
            List<Map<String, Object>> list = new ArrayList<>();
            for (Map<String, Object> row : raw) {
                Map<String, Object> item = new LinkedHashMap<>();
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
                item.put("indexName", name);
                item.put("indexType", indexType);
                item.put("indexMethod", indexMethod);
                item.put("comment", row.get("indexComment"));
                item.put("unique", nonUnique == 0);
                String cols = row.get("columnNames") == null ? "" : String.valueOf(row.get("columnNames"));
                List<String> colList = new ArrayList<>();
                if (StringUtils.isNotEmpty(cols)) {
                    for (String c : cols.split(",")) {
                        if (StringUtils.isNotEmpty(c)) {
                            colList.add(c.trim());
                        }
                    }
                }
                item.put("columns", colList);
                list.add(item);
            }
            return list;
        } finally {
            queryToolSupport.close(tool);
        }
    }

    private String buildCreateIndexSql(String dsType, DsIndexRequest request) {
        if (request.getColumns() == null || request.getColumns().isEmpty()) {
            throw new IllegalArgumentException("索引列不能为空");
        }
        if ("PRIMARY".equalsIgnoreCase(request.getIndexName())) {
            throw new IllegalArgumentException("请使用表主键定义 PRIMARY，不能用此接口创建");
        }
        String indexType = normalizeIndexType(request);
        String indexMethod = normalizeIndexMethod(indexType, request.getIndexMethod());
        String qTable = DsQueryToolSupport.quoteIdent(dsType, request.getTableName());
        String qIndex = DsQueryToolSupport.quoteIdent(dsType, request.getIndexName());
        List<String> cols = new ArrayList<>();
        for (String c : request.getColumns()) {
            cols.add(DsQueryToolSupport.quoteIdent(dsType, c));
        }
        String kind;
        switch (indexType) {
            case "UNIQUE":
                kind = "UNIQUE INDEX";
                break;
            case "FULLTEXT":
                kind = "FULLTEXT INDEX";
                break;
            case "SPATIAL":
                kind = "SPATIAL INDEX";
                break;
            default:
                kind = "INDEX";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE ").append(kind).append(" ").append(qIndex)
                .append(" ON ").append(qTable)
                .append(" (").append(String.join(",", cols)).append(")");
        // FULLTEXT/SPATIAL 不写 USING
        if (("NORMAL".equals(indexType) || "UNIQUE".equals(indexType)) && StringUtils.isNotEmpty(indexMethod)) {
            sb.append(" USING ").append(indexMethod);
        }
        if (StringUtils.isNotEmpty(request.getComment())) {
            sb.append(" COMMENT ").append(quoteLiteral(request.getComment()));
        }
        return sb.toString();
    }

    private String normalizeIndexType(DsIndexRequest request) {
        String t = request.getIndexType();
        if (StringUtils.isEmpty(t) && Boolean.TRUE.equals(request.getUniqueIndex())) {
            return "UNIQUE";
        }
        if (StringUtils.isEmpty(t)) {
            return "NORMAL";
        }
        t = t.trim().toUpperCase(Locale.ROOT);
        if ("NORMAL".equals(t) || "UNIQUE".equals(t) || "FULLTEXT".equals(t) || "SPATIAL".equals(t)) {
            return t;
        }
        throw new IllegalArgumentException("不支持的索引类型：" + request.getIndexType() + "（NORMAL/UNIQUE/FULLTEXT/SPATIAL）");
    }

    private String normalizeIndexMethod(String indexType, String method) {
        if ("FULLTEXT".equals(indexType) || "SPATIAL".equals(indexType)) {
            return "";
        }
        if (StringUtils.isEmpty(method)) {
            return "BTREE";
        }
        String m = method.trim().toUpperCase(Locale.ROOT);
        if ("BTREE".equals(m) || "HASH".equals(m)) {
            return m;
        }
        throw new IllegalArgumentException("不支持的索引方法：" + method + "（BTREE/HASH）");
    }

        private DsJdbcDatasource requireMysql(Long id) {
        DsJdbcDatasource ds = datasourceMapper.selectById(id);
        if (ds == null) {
            throw new IllegalArgumentException("数据源不存在");
        }
        String type = ds.getDatasourceType() == null ? "" : ds.getDatasourceType().toLowerCase(Locale.ROOT);
        boolean ok = false;
        for (String t : MYSQL_TYPES) {
            if (type.contains(t)) {
                ok = true;
                break;
            }
        }
        if (!ok) {
            throw new IllegalArgumentException("表结构管理当前仅支持 MySQL/MariaDB，当前类型：" + ds.getDatasourceType());
        }
        return ds;
    }

    private void assertTableExists(AbstractDsQueryTool tool, String tableName) {
        if (StringUtils.isEmpty(tableName)) {
            throw new IllegalArgumentException("表名不能为空");
        }
        DsQueryToolSupport.quoteIdent("mysql", tableName);
        List<String> tables = tool.getTableNames();
        boolean ok = tables.stream().anyMatch(t -> tableName.equalsIgnoreCase(t));
        if (!ok && !tool.tableExist(tableName)) {
            throw new IllegalArgumentException("表不存在：" + tableName);
        }
    }

    private String buildCreateTableSql(String dsType, String tableName, String comment, List<DsColumnDef> columns) {
        String qTable = DsQueryToolSupport.quoteIdent(dsType, tableName);
        List<String> parts = new ArrayList<>();
        List<String> pkCols = new ArrayList<>();
        for (DsColumnDef col : columns) {
            parts.add(buildColumnClause(dsType, col));
            if (Boolean.TRUE.equals(col.getPrimaryKey())) {
                pkCols.add(DsQueryToolSupport.quoteIdent(dsType, col.getName()));
            }
        }
        if (!pkCols.isEmpty()) {
            parts.add("PRIMARY KEY (" + String.join(",", pkCols) + ")");
        }
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE ").append(qTable).append(" (\n  ");
        sb.append(String.join(",\n  ", parts));
        sb.append("\n) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        if (StringUtils.isNotEmpty(comment)) {
            sb.append(" COMMENT=").append(quoteLiteral(comment));
        }
        return sb.toString();
    }

    private String buildColumnClause(String dsType, DsColumnDef col) {
        if (col == null || StringUtils.isEmpty(col.getName())) {
            throw new IllegalArgumentException("列名不能为空");
        }
        String type = normalizeType(col.getType());
        String qName = DsQueryToolSupport.quoteIdent(dsType, col.getName());
        StringBuilder sb = new StringBuilder();
        sb.append(qName).append(" ").append(type);
        if (Boolean.FALSE.equals(col.getNullable())) {
            sb.append(" NOT NULL");
        } else {
            sb.append(" NULL");
        }
        if (StringUtils.isNotEmpty(col.getDefaultValue())) {
            sb.append(" DEFAULT ").append(normalizeDefault(col.getDefaultValue()));
        }
        if (StringUtils.isNotEmpty(col.getComment())) {
            sb.append(" COMMENT ").append(quoteLiteral(col.getComment()));
        }
        return sb.toString();
    }

    private String normalizeType(String type) {
        if (StringUtils.isEmpty(type)) {
            throw new IllegalArgumentException("字段类型不能为空");
        }
        String t = type.trim().toLowerCase(Locale.ROOT).replace(" ", "");
        Matcher m = TYPE_PATTERN.matcher(t);
        if (!m.matches()) {
            throw new IllegalArgumentException("不支持的字段类型：" + type
                    + "（示例：varchar(64)、bigint、decimal(10,2)、datetime）");
        }
        return t;
    }

    /** 默认值：NULL / CURRENT_TIMESTAMP / 数字 / 已加引号字符串 / 普通字符串自动加引号 */
    private String normalizeDefault(String raw) {
        String v = raw.trim();
        if ("null".equalsIgnoreCase(v)) {
            return "NULL";
        }
        if ("current_timestamp".equalsIgnoreCase(v)) {
            return "CURRENT_TIMESTAMP";
        }
        if ((v.startsWith("'") && v.endsWith("'")) || (v.startsWith("\"") && v.endsWith("\""))) {
            return quoteLiteral(v.substring(1, v.length() - 1));
        }
        if (v.matches("-?\\d+(\\.\\d+)?")) {
            return v;
        }
        return quoteLiteral(v);
    }

    private String quoteLiteral(String s) {
        return "'" + s.replace("\\", "\\\\").replace("'", "''") + "'";
    }

    private Integer toInt(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number) {
            return ((Number) v).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (Exception e) {
            return null;
        }
    }
}
