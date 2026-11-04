package com.winter.cloud.datasource.service.impl;

import com.alibaba.druid.util.JdbcUtils;
import com.winter.cloud.common.core.utils.StringUtils;
import com.winter.cloud.common.datasource.info.ColumnInfo;
import com.winter.cloud.common.datasource.query.AbstractDsQueryTool;
import com.winter.cloud.datasource.domain.dto.DsTableRowRequest;
import com.winter.cloud.datasource.domain.entity.DsJdbcDatasource;
import com.winter.cloud.datasource.mapper.DsJdbcDatasourceMapper;
import com.winter.cloud.datasource.service.IDsTableDataService;
import com.winter.cloud.datasource.support.DsQueryToolSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 远程库表数据 CRUD（参数化 + 标识符白名单）
 */
@Service
public class DsTableDataServiceImpl implements IDsTableDataService {

    private static final int MAX_PAGE_SIZE = 200;

    @Autowired
    private DsQueryToolSupport queryToolSupport;

    @Autowired
    private DsJdbcDatasourceMapper datasourceMapper;

    @Override
    public List<String> listTables(Long datasourceId, String tableName) {
        AbstractDsQueryTool tool = queryToolSupport.open(datasourceId);
        try {
            List<String> tables = tool.getTableNames();
            if (StringUtils.isEmpty(tableName)) {
                return tables;
            }
            String kw = tableName.trim().toLowerCase(Locale.ROOT);
            return tables.stream()
                    .filter(t -> t != null && t.toLowerCase(Locale.ROOT).contains(kw))
                    .collect(Collectors.toList());
        } finally {
            queryToolSupport.close(tool);
        }
    }

    @Override
    public List<ColumnInfo> listColumns(Long datasourceId, String tableName) {
        AbstractDsQueryTool tool = queryToolSupport.open(datasourceId);
        try {
            assertTableExists(tool, tableName);
            List<ColumnInfo> columns = tool.getColumnInfos(tableName);
            // MetaData 主键可能不准（复合主键会被覆盖），再补一次
            markPrimaryKeys(tool, tableName, columns);
            return columns;
        } finally {
            queryToolSupport.close(tool);
        }
    }

    @Override
    public Map<String, Object> pageRows(Long datasourceId, String tableName, int pageNum, int pageSize) {
        if (pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize < 1) {
            pageSize = 10;
        }
        if (pageSize > MAX_PAGE_SIZE) {
            pageSize = MAX_PAGE_SIZE;
        }
        DsJdbcDatasource ds = requireDatasource(datasourceId);
        AbstractDsQueryTool tool = queryToolSupport.open(datasourceId);
        try {
            assertTableExists(tool, tableName);
            String qTable = DsQueryToolSupport.quoteIdent(ds.getDatasourceType(), tableName);
            JdbcTemplate jdbc = tool.getJdbcTemplate();
            Long total = jdbc.queryForObject("SELECT COUNT(*) FROM " + qTable, Long.class);
            if (total == null) {
                total = 0L;
            }
            int offset = (pageNum - 1) * pageSize;
            String pageSql = buildPageSql(ds.getDatasourceType(), qTable, offset, pageSize);
            List<Map<String, Object>> rows = jdbc.queryForList(pageSql);
            List<Map<String, Object>> normalized = new ArrayList<>(rows.size());
            for (Map<String, Object> row : rows) {
                normalized.add(normalizeRow(row));
            }
            Map<String, Object> result = new LinkedHashMap<>(4);
            result.put("total", total);
            result.put("rows", normalized);
            return result;
        } finally {
            queryToolSupport.close(tool);
        }
    }

    @Override
    public int insertRow(DsTableRowRequest request) {
        Map<String, Object> row = request.getRow();
        if (row == null || row.isEmpty()) {
            throw new IllegalArgumentException("行数据不能为空");
        }
        DsJdbcDatasource ds = requireDatasource(request.getDatasourceId());
        AbstractDsQueryTool tool = queryToolSupport.open(request.getDatasourceId());
        try {
            assertTableExists(tool, request.getTableName());
            Set<String> allowed = columnNameSet(tool.getColumnInfos(request.getTableName()));
            List<String> cols = new ArrayList<>();
            List<Object> vals = new ArrayList<>();
            for (Map.Entry<String, Object> e : row.entrySet()) {
                if (!allowed.contains(e.getKey())) {
                    throw new IllegalArgumentException("未知列：" + e.getKey());
                }
                cols.add(DsQueryToolSupport.quoteIdent(ds.getDatasourceType(), e.getKey()));
                vals.add(e.getValue());
            }
            if (cols.isEmpty()) {
                throw new IllegalArgumentException("无有效字段");
            }
            String qTable = DsQueryToolSupport.quoteIdent(ds.getDatasourceType(), request.getTableName());
            String placeholders = String.join(",", Collections.nCopies(cols.size(), "?"));
            String sql = "INSERT INTO " + qTable + " (" + String.join(",", cols) + ") VALUES (" + placeholders + ")";
            return tool.getJdbcTemplate().update(sql, vals.toArray());
        } finally {
            queryToolSupport.close(tool);
        }
    }

    @Override
    public int updateRow(DsTableRowRequest request) {
        Map<String, Object> pk = request.getPk();
        Map<String, Object> row = request.getRow();
        if (pk == null || pk.isEmpty()) {
            throw new IllegalArgumentException("缺少主键条件，无法更新");
        }
        if (row == null || row.isEmpty()) {
            throw new IllegalArgumentException("更新字段不能为空");
        }
        DsJdbcDatasource ds = requireDatasource(request.getDatasourceId());
        AbstractDsQueryTool tool = queryToolSupport.open(request.getDatasourceId());
        try {
            assertTableExists(tool, request.getTableName());
            Set<String> allowed = columnNameSet(tool.getColumnInfos(request.getTableName()));
            Set<String> pkCols = loadPrimaryKeyNames(tool, request.getTableName());
            if (pkCols.isEmpty()) {
                throw new IllegalArgumentException("该表无主键，不支持更新");
            }
            for (String k : pk.keySet()) {
                if (!pkCols.contains(k)) {
                    throw new IllegalArgumentException("非主键列不可作条件：" + k);
                }
            }
            List<String> setParts = new ArrayList<>();
            List<Object> args = new ArrayList<>();
            for (Map.Entry<String, Object> e : row.entrySet()) {
                if (pkCols.contains(e.getKey())) {
                    // 主键不参与 SET
                    continue;
                }
                if (!allowed.contains(e.getKey())) {
                    throw new IllegalArgumentException("未知列：" + e.getKey());
                }
                setParts.add(DsQueryToolSupport.quoteIdent(ds.getDatasourceType(), e.getKey()) + "=?");
                args.add(e.getValue());
            }
            if (setParts.isEmpty()) {
                throw new IllegalArgumentException("无有效更新字段");
            }
            List<String> whereParts = new ArrayList<>();
            for (String pkCol : pkCols) {
                if (!pk.containsKey(pkCol)) {
                    throw new IllegalArgumentException("主键条件不完整：" + pkCol);
                }
                whereParts.add(DsQueryToolSupport.quoteIdent(ds.getDatasourceType(), pkCol) + "=?");
                args.add(pk.get(pkCol));
            }
            String qTable = DsQueryToolSupport.quoteIdent(ds.getDatasourceType(), request.getTableName());
            String sql = "UPDATE " + qTable + " SET " + String.join(",", setParts)
                    + " WHERE " + String.join(" AND ", whereParts);
            return tool.getJdbcTemplate().update(sql, args.toArray());
        } finally {
            queryToolSupport.close(tool);
        }
    }

    @Override
    public int deleteRow(DsTableRowRequest request) {
        Map<String, Object> pk = request.getPk();
        if (pk == null || pk.isEmpty()) {
            throw new IllegalArgumentException("缺少主键条件，无法删除");
        }
        DsJdbcDatasource ds = requireDatasource(request.getDatasourceId());
        AbstractDsQueryTool tool = queryToolSupport.open(request.getDatasourceId());
        try {
            assertTableExists(tool, request.getTableName());
            Set<String> pkCols = loadPrimaryKeyNames(tool, request.getTableName());
            if (pkCols.isEmpty()) {
                throw new IllegalArgumentException("该表无主键，不支持删除");
            }
            List<String> whereParts = new ArrayList<>();
            List<Object> args = new ArrayList<>();
            for (String pkCol : pkCols) {
                if (!pk.containsKey(pkCol)) {
                    throw new IllegalArgumentException("主键条件不完整：" + pkCol);
                }
                whereParts.add(DsQueryToolSupport.quoteIdent(ds.getDatasourceType(), pkCol) + "=?");
                args.add(pk.get(pkCol));
            }
            String qTable = DsQueryToolSupport.quoteIdent(ds.getDatasourceType(), request.getTableName());
            String sql = "DELETE FROM " + qTable + " WHERE " + String.join(" AND ", whereParts);
            return tool.getJdbcTemplate().update(sql, args.toArray());
        } finally {
            queryToolSupport.close(tool);
        }
    }

    private DsJdbcDatasource requireDatasource(Long id) {
        DsJdbcDatasource ds = datasourceMapper.selectById(id);
        if (ds == null) {
            throw new IllegalArgumentException("数据源不存在");
        }
        return ds;
    }

    private void assertTableExists(AbstractDsQueryTool tool, String tableName) {
        if (StringUtils.isEmpty(tableName)) {
            throw new IllegalArgumentException("表名不能为空");
        }
        // 先走白名单校验字符
        DsQueryToolSupport.quoteIdent("mysql", tableName);
        List<String> tables = tool.getTableNames();
        boolean ok = tables.stream().anyMatch(t -> tableName.equalsIgnoreCase(t));
        if (!ok && !tool.tableExist(tableName)) {
            throw new IllegalArgumentException("表不存在：" + tableName);
        }
    }

    private Set<String> columnNameSet(List<ColumnInfo> columns) {
        Set<String> set = new LinkedHashSet<>();
        if (columns != null) {
            for (ColumnInfo c : columns) {
                if (c != null && c.getName() != null) {
                    set.add(c.getName());
                }
            }
        }
        return set;
    }

    private void markPrimaryKeys(AbstractDsQueryTool tool, String tableName, List<ColumnInfo> columns) {
        Set<String> pks = loadPrimaryKeyNames(tool, tableName);
        if (pks.isEmpty() || columns == null) {
            return;
        }
        for (ColumnInfo c : columns) {
            c.setPrimarykey(pks.contains(c.getName()));
        }
    }

    private Set<String> loadPrimaryKeyNames(AbstractDsQueryTool tool, String tableName) {
        Set<String> pks = new LinkedHashSet<>();
        Connection conn = null;
        ResultSet rs = null;
        try {
            conn = tool.getJdbcTemplate().getDataSource().getConnection();
            rs = conn.getMetaData().getPrimaryKeys(conn.getCatalog(), null, tableName);
            while (rs.next()) {
                String name = rs.getString("COLUMN_NAME");
                if (StringUtils.isNotEmpty(name)) {
                    pks.add(name);
                }
            }
            // MySQL 在部分驱动下 catalog 不一致，再试 schema/null
            if (pks.isEmpty()) {
                JdbcUtils.close(rs);
                rs = conn.getMetaData().getPrimaryKeys(null, null, tableName);
                while (rs.next()) {
                    String name = rs.getString("COLUMN_NAME");
                    if (StringUtils.isNotEmpty(name)) {
                        pks.add(name);
                    }
                }
            }
        } catch (SQLException e) {
            // 无主键或驱动不支持时返回空集
        } finally {
            JdbcUtils.close(rs);
            JdbcUtils.close(conn);
        }
        return pks;
    }

    private String buildPageSql(String datasourceType, String quotedTable, int offset, int pageSize) {
        String type = datasourceType == null ? "" : datasourceType.toLowerCase(Locale.ROOT);
        if (type.contains("oracle")) {
            int end = offset + pageSize;
            return "SELECT * FROM (SELECT t.*, ROWNUM rn FROM (SELECT * FROM " + quotedTable
                    + ") t WHERE ROWNUM <= " + end + ") WHERE rn > " + offset;
        }
        if (type.contains("sqlserver")) {
            // 无稳定排序时用 OFFSET/FETCH（SQL Server 2012+）
            return "SELECT * FROM " + quotedTable + " ORDER BY (SELECT NULL) OFFSET " + offset
                    + " ROWS FETCH NEXT " + pageSize + " ROWS ONLY";
        }
        // mysql / postgresql / 其它：LIMIT
        return "SELECT * FROM " + quotedTable + " LIMIT " + pageSize + " OFFSET " + offset;
    }

    private Map<String, Object> normalizeRow(Map<String, Object> row) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (row == null) {
            return out;
        }
        for (Map.Entry<String, Object> e : row.entrySet()) {
            Object v = e.getValue();
            if (v == null) {
                out.put(e.getKey(), null);
            } else if (v instanceof byte[]) {
                out.put(e.getKey(), "[BLOB " + ((byte[]) v).length + " bytes]");
            } else if (v instanceof java.sql.Blob) {
                out.put(e.getKey(), "[BLOB]");
            } else if (v instanceof java.sql.Clob) {
                out.put(e.getKey(), "[CLOB]");
            } else if (v instanceof Timestamp) {
                out.put(e.getKey(), v.toString());
            } else if (v instanceof Date) {
                out.put(e.getKey(), v.toString());
            } else {
                out.put(e.getKey(), v);
            }
        }
        return out;
    }
}
