package com.winter.cloud.common.datasource.sqlbuilder;

import com.alibaba.druid.util.JdbcConstants;
import com.winter.cloud.common.core.utils.StringUtils;

/**
 * SQLServer sqlBuilder
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2023/4/20 13:55
 */
public class SQLServerSqlBuilder extends AbstractSqlBuidler implements SqlBuilder {

    private volatile static SQLServerSqlBuilder single;

    public static SQLServerSqlBuilder getInstance() {
        if (single == null) {
            synchronized (SQLServerSqlBuilder.class) {
                if (single == null) {
                    single = new SQLServerSqlBuilder();
                }
            }
        }
        return single;
    }

    @Override
    public String getDatasourceType() {
        return JdbcConstants.SQL_SERVER.name();
    }

    @Override
    public String queryTables() {
        return "SELECT Name FROM SysObjects Where XType='U' ORDER BY Name";
    }

    @Override
    public String queryTables(String tableSchema) {
        return "select schema_name(schema_id)+'.'+object_name(object_id) from sys.objects \n" +
                "where type ='U' \n" +
                "and schema_name(schema_id) ='" + tableSchema + "'";
    }

    @Override
    public String queryTableTotal(String schemaName, String tableName) {
        String tableNameSql = StringUtils.isNotEmpty(tableName) ? " AND name LIKE '%" + tableName + "%' " : "";
        return String.format("SELECT count(*) AS total FROM SysObjects WHERE XType='U' %s ", tableNameSql);
    }

    @Override
    public String pageQueryTables(String schemaName, String tableName, int currentPage, int pageSize) {
        String tableNameSql = StringUtils.isNotEmpty(tableName) ? " AND name LIKE '%" + tableName + "%' " : "";
        return String.format("SELECT TOP %d name FROM ( " +
                "SELECT row_number() OVER ( ORDER BY id ASC ) AS row_number, name FROM SysObjects " +
                "WHERE XType = 'U' %s) " +
                "temp_row WHERE row_number > ( ( %d-1 ) * %d )", pageSize, tableNameSql, currentPage, pageSize);
    }

    @Override
    public String queryTableSchema(String... args) {
        return "select distinct schema_name(schema_id) from sys.objects where type ='U'";
    }

    @Override
    public String queryDataByLimit(String schemaName, String tableName, int limit) {
        return String.format("SELECT TOP %d * FROM %s ", limit, tableName);
    }
}
