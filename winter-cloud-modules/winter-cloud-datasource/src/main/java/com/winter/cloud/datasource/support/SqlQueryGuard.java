package com.winter.cloud.datasource.support;

import com.winter.cloud.common.core.utils.StringUtils;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Guard for custom SQL: single readonly statement only
 */
public final class SqlQueryGuard {

    private static final int MAX_SQL_LENGTH = 20000;

    private static final Pattern BLOCK_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
    private static final Pattern LINE_COMMENT = Pattern.compile("(--|#)[^\\n]*");

    private static final Pattern FORBIDDEN = Pattern.compile(
            "(?i)\\b(insert|update|delete|merge|replace|upsert|truncate|drop|alter|create|rename|grant|revoke|"
                    + "call|execute|exec|load|import|export|copy|into\\s+outfile|into\\s+dumpfile|"
                    + "set\\s+password|lock\\s+tables|unlock\\s+tables)\\b");

    private SqlQueryGuard() {
    }

    public static String validateReadonly(String rawSql) {
        if (StringUtils.isEmpty(rawSql)) {
            throw new IllegalArgumentException("\u0053\u0051\u004c \u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (rawSql.length() > MAX_SQL_LENGTH) {
            throw new IllegalArgumentException("\u0053\u0051\u004c \u8fc7\u957f\uff0c\u6700\u5927 " + MAX_SQL_LENGTH + " \u5b57\u7b26");
        }
        String sql = stripComments(rawSql).trim();
        if (sql.isEmpty()) {
            throw new IllegalArgumentException("\u0053\u0051\u004c \u4e0d\u80fd\u4e3a\u7a7a");
        }
        String body = stripTrailingSemicolon(sql);
        if (body.indexOf(';') >= 0) {
            throw new IllegalArgumentException("\u4ec5\u652f\u6301\u5355\u6761 \u0053\u0051\u004c\uff0c\u8bf7\u53bb\u6389\u591a\u4f59\u5206\u53f7");
        }
        if (FORBIDDEN.matcher(body).find()) {
            throw new IllegalArgumentException("\u4ec5\u5141\u8bb8\u53ea\u8bfb\u67e5\u8be2\uff08SELECT/WITH/SHOW/DESCRIBE/EXPLAIN\uff09\uff0c\u7981\u6b62\u5199\u64cd\u4f5c\u4e0e DDL");
        }
        String first = firstKeyword(body);
        if (!isReadonlyKeyword(first)) {
            throw new IllegalArgumentException("\u4ec5\u5141\u8bb8\u4ee5 SELECT / WITH / SHOW / DESCRIBE / DESC / EXPLAIN \u5f00\u5934\u7684\u8bed\u53e5");
        }
        return body;
    }

    private static String stripComments(String sql) {
        String s = BLOCK_COMMENT.matcher(sql).replaceAll(" ");
        s = LINE_COMMENT.matcher(s).replaceAll(" ");
        return s;
    }

    private static String stripTrailingSemicolon(String sql) {
        String s = sql.trim();
        while (s.endsWith(";")) {
            s = s.substring(0, s.length() - 1).trim();
        }
        return s;
    }

    private static String firstKeyword(String sql) {
        int i = 0;
        while (i < sql.length() && Character.isWhitespace(sql.charAt(i))) {
            i++;
        }
        int j = i;
        while (j < sql.length()) {
            char c = sql.charAt(j);
            if (!Character.isLetter(c) && c != '_') {
                break;
            }
            j++;
        }
        return sql.substring(i, j).toUpperCase(Locale.ROOT);
    }

    private static boolean isReadonlyKeyword(String kw) {
        return "SELECT".equals(kw) || "WITH".equals(kw) || "SHOW".equals(kw)
                || "DESCRIBE".equals(kw) || "DESC".equals(kw) || "EXPLAIN".equals(kw);
    }
}