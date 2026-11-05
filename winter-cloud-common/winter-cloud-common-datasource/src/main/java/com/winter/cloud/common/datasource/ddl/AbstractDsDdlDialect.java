package com.winter.cloud.common.datasource.ddl;

import com.winter.cloud.common.core.utils.StringUtils;
import com.winter.cloud.common.datasource.ddl.model.DsColumnDef;
import com.winter.cloud.common.datasource.ddl.model.DsIndexRequest;
import com.winter.cloud.common.datasource.util.JdbcIdentUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * DDL 方言公共能力
 */
public abstract class AbstractDsDdlDialect implements DsDdlDialect {

    protected abstract Pattern typePattern();

    protected abstract String typeHint();

    protected String dsKey() {
        return name();
    }

    protected String quote(String ident) {
        return JdbcIdentUtils.quoteIdent(dsKey(), ident);
    }

    protected String normalizeType(String type) {
        if (StringUtils.isEmpty(type)) {
            throw new IllegalArgumentException("字段类型不能为空");
        }
        String compact = type.trim().toLowerCase(Locale.ROOT).replace(" ", "");
        if (!typePattern().matcher(compact).matches()) {
            throw new IllegalArgumentException("不支持的字段类型：" + type + "（" + typeHint() + "）");
        }
        return mapCompactType(compact);
    }

    protected String mapCompactType(String compact) {
        if ("doubleprecision".equals(compact)) {
            return "double precision";
        }
        return compact;
    }

    protected String normalizeDefault(String raw) {
        String v = raw.trim();
        if ("null".equalsIgnoreCase(v)) {
            return "NULL";
        }
        if ("current_timestamp".equalsIgnoreCase(v) || "sysdate".equalsIgnoreCase(v) || "getdate()".equalsIgnoreCase(v)) {
            return mapCurrentTimestamp(v);
        }
        if ((v.startsWith("'") && v.endsWith("'")) || (v.startsWith("\"") && v.endsWith("\""))) {
            return quoteLiteral(v.substring(1, v.length() - 1));
        }
        if (v.matches("-?\\d+(\\.\\d+)?")) {
            return v;
        }
        return quoteLiteral(v);
    }

    protected String mapCurrentTimestamp(String v) {
        return "CURRENT_TIMESTAMP";
    }

    protected String quoteLiteral(String s) {
        return "'" + s.replace("'", "''") + "'";
    }

    protected String normalizeIndexType(DsIndexRequest request) {
        String t = request.getIndexType();
        if (StringUtils.isEmpty(t) && Boolean.TRUE.equals(request.getUniqueIndex())) {
            return "UNIQUE";
        }
        if (StringUtils.isEmpty(t)) {
            return "NORMAL";
        }
        t = t.trim().toUpperCase(Locale.ROOT);
        if (!supportedIndexTypes().contains(t) && !"PRIMARY".equals(t)) {
            throw new IllegalArgumentException(name() + " 不支持索引类型：" + request.getIndexType()
                    + "（支持：" + String.join("/", supportedIndexTypes()) + "）");
        }
        return t;
    }

    protected String normalizeIndexMethod(String indexType, String method) {
        if ("FULLTEXT".equals(indexType) || "SPATIAL".equals(indexType)) {
            return "";
        }
        List<String> methods = supportedIndexMethods();
        if (methods == null || methods.isEmpty()) {
            return "";
        }
        if (StringUtils.isEmpty(method)) {
            return methods.get(0);
        }
        String m = method.trim().toUpperCase(Locale.ROOT);
        if (!methods.contains(m)) {
            throw new IllegalArgumentException(name() + " 不支持索引方法：" + method
                    + "（支持：" + String.join("/", methods) + "）");
        }
        return m;
    }

    protected List<String> buildPkColumnClauses(List<DsColumnDef> columns) {
        List<String> parts = new ArrayList<>();
        List<String> pkCols = new ArrayList<>();
        for (DsColumnDef col : columns) {
            parts.add(buildInlineColumnClause(col, true));
            if (Boolean.TRUE.equals(col.getPrimaryKey())) {
                pkCols.add(quote(col.getName()));
            }
        }
        if (!pkCols.isEmpty()) {
            parts.add("PRIMARY KEY (" + String.join(",", pkCols) + ")");
        }
        return parts;
    }

    /**
     * @param inlineComment 是否把注释写在列定义里（MySQL）；PG/Oracle 用 COMMENT ON
     */
    protected String buildInlineColumnClause(DsColumnDef col, boolean inlineComment) {
        if (col == null || StringUtils.isEmpty(col.getName())) {
            throw new IllegalArgumentException("列名不能为空");
        }
        String type = normalizeType(col.getType());
        StringBuilder sb = new StringBuilder();
        sb.append(quote(col.getName())).append(" ").append(type);
        if (Boolean.FALSE.equals(col.getNullable())) {
            sb.append(" NOT NULL");
        } else {
            sb.append(" NULL");
        }
        if (StringUtils.isNotEmpty(col.getDefaultValue())) {
            sb.append(" DEFAULT ").append(normalizeDefault(col.getDefaultValue()));
        }
        if (inlineComment && StringUtils.isNotEmpty(col.getComment())) {
            sb.append(" COMMENT ").append(quoteLiteral(col.getComment()));
        }
        return sb.toString();
    }

    protected Map<String, Object> indexItem(String name, String indexType, String indexMethod,
                                          String comment, List<String> columns, boolean unique) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("indexName", name);
        item.put("indexType", indexType);
        item.put("indexMethod", indexMethod == null ? "" : indexMethod);
        item.put("comment", comment == null ? "" : comment);
        item.put("unique", unique);
        item.put("columns", columns == null ? new ArrayList<>() : columns);
        return item;
    }

    protected List<String> splitCsv(Object csv) {
        List<String> colList = new ArrayList<>();
        if (csv == null) {
            return colList;
        }
        for (String c : String.valueOf(csv).split(",")) {
            if (StringUtils.isNotEmpty(c)) {
                colList.add(c.trim());
            }
        }
        return colList;
    }

    protected Integer toInt(Object v) {
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

    @Override
    public List<String> supportedIndexTypes() {
        return Arrays.asList("NORMAL", "UNIQUE");
    }

    @Override
    public List<String> supportedIndexMethods() {
        return Arrays.asList("BTREE");
    }

    @Override
    public boolean supportsIndexComment() {
        return false;
    }
}
