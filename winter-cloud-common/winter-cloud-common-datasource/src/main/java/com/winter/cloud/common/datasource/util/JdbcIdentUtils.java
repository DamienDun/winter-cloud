package com.winter.cloud.common.datasource.util;

import com.winter.cloud.common.core.utils.StringUtils;

/**
 * JDBC 标识符校验与按方言加引号
 */
public final class JdbcIdentUtils {

    private JdbcIdentUtils() {
    }

    public static String quoteIdent(String datasourceType, String name) {
        if (StringUtils.isEmpty(name)) {
            throw new IllegalArgumentException("\u6807\u8bc6\u7b26\u4e0d\u80fd\u4e3a\u7a7a");
        }
        String n = name.trim();
        for (int i = 0; i < n.length(); i++) {
            char c = n.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9') || c == '_' || c == '$' || c == '#';
            if (!ok) {
                throw new IllegalArgumentException("\u975e\u6cd5\u6807\u8bc6\u7b26\uff1a" + name);
            }
        }
        String type = datasourceType == null ? "" : datasourceType.toLowerCase();
        if (type.contains("mysql") || type.contains("hive") || type.contains("clickhouse") || type.contains("mariadb")) {
            return "" + n.replace("", "`") + "";
        }
        if (type.contains("sqlserver")) {
            return "[" + n.replace("]", "]]") + "]";
        }
        return "\"" + n.replace("\"", "\"\"") + "\"";
    }
}