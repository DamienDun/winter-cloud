package com.winter.cloud.common.datasource.sqlbuilder;

import com.alibaba.druid.util.JdbcConstants;
import com.winter.cloud.common.core.utils.StringUtils;

/**
 * mysql sql builder
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2023/4/20 13:52
 */
public class MySQLSqlBuilder extends AbstractSqlBuidler implements SqlBuilder {

    private volatile static MySQLSqlBuilder single;

    public static MySQLSqlBuilder getInstance() {
        if (single == null) {
            synchronized (MySQLSqlBuilder.class) {
                if (single == null) {
                    single = new MySQLSqlBuilder();
                }
            }
        }
        return single;
    }

    @Override
    public String getDatasourceType() {
        return JdbcConstants.MYSQL.name();
    }

    @Override
    public String queryFieldComment(String schemaName, String tableName, String columnName) {
        return String.format("SELECT COLUMN_COMMENT FROM information_schema.COLUMNS where TABLE_SCHEMA = '%s' and TABLE_NAME = '%s' and COLUMN_NAME = '%s'", schemaName, tableName, columnName);
    }

    @Override
    public String queryPrimaryKey() {
        return "select column_name from information_schema.columns where table_schema=? and table_name=? and column_key = 'PRI'";
    }

    @Override
    public String queryTableTotal(String schemaName, String tableName) {
        String tableNameSql = StringUtils.isNotEmpty(tableName) ? " AND table_name LIKE '%" + tableName + "%' " : "";
        return String.format("SELECT count(*) FROM information_schema.TABLES WHERE table_schema = '%s' %s ", schemaName, tableNameSql);
    }

    @Override
    public String pageQueryTables(String schemaName, String tableName, int currentPage, int pageSize) {
        int offset = (currentPage - 1) * pageSize;
        String tableNameSql = StringUtils.isNotEmpty(tableName) ? " and table_name like CONCAT('%','" + tableName + "','%') " : "";
        return String.format("SELECT table_name FROM information_schema.tables WHERE table_schema = '%s' %s LIMIT %d, %d ",
                schemaName, tableNameSql, offset, pageSize);
    }

    @Override
    public String queryDataByLimit(String schemaName, String tableName, int limit) {
        return String.format("SELECT * FROM `%s`.`%s` LIMIT %d", schemaName, tableName, limit);
    }
}

