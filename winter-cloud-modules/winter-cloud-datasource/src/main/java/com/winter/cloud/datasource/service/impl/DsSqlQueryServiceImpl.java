package com.winter.cloud.datasource.service.impl;

import com.winter.cloud.common.datasource.query.AbstractDsQueryTool;
import com.winter.cloud.datasource.domain.dto.DsSqlQueryRequest;
import com.winter.cloud.datasource.service.IDsSqlQueryService;
import com.winter.cloud.datasource.support.DsQueryToolSupport;
import com.winter.cloud.datasource.support.SqlQueryGuard;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Execute readonly SQL on target datasource with row/timeout limits
 */
@Service
public class DsSqlQueryServiceImpl implements IDsSqlQueryService {

    private static final int DEFAULT_MAX_ROWS = 200;
    private static final int HARD_MAX_ROWS = 1000;
    private static final int QUERY_TIMEOUT_SEC = 30;

    @Autowired
    private DsQueryToolSupport queryToolSupport;

    @Override
    public Map<String, Object> query(DsSqlQueryRequest request) {
        String sql = SqlQueryGuard.validateReadonly(request.getSql());
        int maxRows = normalizeMaxRows(request.getMaxRows());

        AbstractDsQueryTool tool = queryToolSupport.open(request.getDatasourceId());
        long start = System.currentTimeMillis();
        try {
            return doQuery(tool, sql, maxRows, start);
        } catch (SQLException e) {
            throw new IllegalStateException("\u0053\u0051\u004c \u6267\u884c\u5931\u8d25\uff1a" + e.getMessage(), e);
        } finally {
            queryToolSupport.close(tool);
        }
    }

    private Map<String, Object> doQuery(AbstractDsQueryTool tool, String sql, int maxRows, long start)
            throws SQLException {
        Connection conn = tool.getJdbcTemplate().getDataSource().getConnection();
        List<String> columns = new ArrayList<>();
        List<Map<String, Object>> rows = new ArrayList<>();
        boolean truncated = false;
        try (Statement stmt = conn.createStatement()) {
            stmt.setMaxRows(maxRows + 1);
            stmt.setQueryTimeout(QUERY_TIMEOUT_SEC);
            boolean hasResult = stmt.execute(sql);
            if (!hasResult) {
                throw new IllegalArgumentException("\u8bed\u53e5\u672a\u8fd4\u56de\u7ed3\u679c\u96c6\uff0c\u8bf7\u4f7f\u7528\u67e5\u8be2\u7c7b \u0053\u0051\u004c");
            }
            try (ResultSet rs = stmt.getResultSet()) {
                ResultSetMetaData meta = rs.getMetaData();
                int colCount = meta.getColumnCount();
                for (int i = 1; i <= colCount; i++) {
                    String label = meta.getColumnLabel(i);
                    if (label == null || label.isEmpty()) {
                        label = meta.getColumnName(i);
                    }
                    columns.add(label);
                }
                while (rs.next()) {
                    if (rows.size() >= maxRows) {
                        truncated = true;
                        break;
                    }
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= colCount; i++) {
                        row.put(columns.get(i - 1), readCell(rs, i, meta.getColumnType(i)));
                    }
                    rows.add(row);
                }
            }
        } finally {
            try {
                conn.close();
            } catch (SQLException ignored) {
                // ignore
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("columns", columns);
        result.put("rows", rows);
        result.put("rowCount", rows.size());
        result.put("maxRows", maxRows);
        result.put("truncated", truncated);
        result.put("costMs", System.currentTimeMillis() - start);
        return result;
    }

    private Object readCell(ResultSet rs, int index, int sqlType) throws SQLException {
        Object v = rs.getObject(index);
        if (v == null) {
            return null;
        }
        if (sqlType == Types.BLOB || sqlType == Types.BINARY || sqlType == Types.VARBINARY
                || sqlType == Types.LONGVARBINARY) {
            return "[BINARY]";
        }
        if (sqlType == Types.CLOB || sqlType == Types.NCLOB || sqlType == Types.LONGVARCHAR
                || sqlType == Types.LONGNVARCHAR) {
            String s = rs.getString(index);
            if (s != null && s.length() > 4000) {
                return s.substring(0, 4000) + "...";
            }
            return s;
        }
        if (v instanceof byte[]) {
            return "[BINARY]";
        }
        return v;
    }

    private int normalizeMaxRows(Integer maxRows) {
        if (maxRows == null || maxRows <= 0) {
            return DEFAULT_MAX_ROWS;
        }
        return Math.min(maxRows, HARD_MAX_ROWS);
    }
}