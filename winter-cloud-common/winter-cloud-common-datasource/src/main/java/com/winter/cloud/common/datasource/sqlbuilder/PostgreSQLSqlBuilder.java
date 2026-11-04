package com.winter.cloud.common.datasource.sqlbuilder;

import com.alibaba.druid.util.JdbcConstants;
import com.winter.cloud.common.core.utils.StringUtils;

/**
 * PostgreSQL sqlBuilder
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2023/4/20 13:54
 */
public class PostgreSQLSqlBuilder extends AbstractSqlBuidler implements SqlBuilder {

    private volatile static PostgreSQLSqlBuilder single;

    public static PostgreSQLSqlBuilder getInstance() {
        if (single == null) {
            synchronized (PostgreSQLSqlBuilder.class) {
                if (single == null) {
                    single = new PostgreSQLSqlBuilder();
                }
            }
        }
        return single;
    }

    @Override
    public String getDatasourceType() {
        return JdbcConstants.POSTGRESQL.name();
    }

    @Override
    public String queryPrimaryKey() {
        return "select column_name from information_schema.columns where table_schema='public' and table_name='tb_cis_patient_info' and is_identity = 'YES'";
    }

    @Override
    public String queryTables() {
        return "select relname as tabname from pg_class c \n" +
                "where  relkind = 'r' and relname not like 'pg_%' and relname not like 'sql_%' group by relname order by relname";
    }

    @Override
    public String queryTables(String tableSchema) {
        return "SELECT concat_ws('.',\"table_schema\",\"table_name\") FROM information_schema.tables \n" +
                "where (\"table_name\" not like 'pg_%' AND \"table_name\" not like 'sql_%') \n" +
                "and table_type='BASE TABLE' and table_schema='" + tableSchema + "'";
    }

    @Override
    public String queryDatabases() {
        return "SELECT datname FROM pg_database";
    }

    @Override
    public String queryTableSchema(String... args) {
        return "select table_schema FROM information_schema.tables where \"table_name\" not like 'pg_%' or \"table_name\" not like 'sql_%' group by table_schema;";
    }

    @Override
    public String queryTableTotal(String schemaName, String tableName) {
        String tableNameSql = StringUtils.isNotEmpty(tableName) ? " and relname like '%" + tableName + "%' " : " ";
        return "SELECT count(*) FROM (select relname as tabname from pg_class c " +
                "where relkind = 'r' and relname not like 'pg_%' and relname not like 'sql_%' " + tableNameSql + " group by relname order by relname) AS temp ";
    }

    @Override
    public String pageQueryTables(String schemaName, String tableName, int currentPage, int pageSize) {
        int offset = (currentPage - 1) * pageSize;
        String tableNameSql = StringUtils.isNotEmpty(tableName) ? " and relname like '%" + tableName + "%' " : " ";
        return "select relname as tabname from pg_class where  relkind = 'r' and relname not like 'pg_%' and relname not like 'sql_%' "
                + tableNameSql + "group by relname order by relname LIMIT " + pageSize + " offset " + offset;
    }

    @Override
    public String queryDataByLimit(String schemaName, String tableName, int limit) {
        return String.format("SELECT * FROM %s LIMIT %d", tableName, limit);
    }
}
