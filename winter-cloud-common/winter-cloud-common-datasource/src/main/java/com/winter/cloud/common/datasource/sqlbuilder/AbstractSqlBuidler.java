package com.winter.cloud.common.datasource.sqlbuilder;

/**
 * 抽象 sql builder
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2023/4/20 13:50
 */
public abstract class AbstractSqlBuidler implements SqlBuilder {

    @Override
    public String queryFields(String tableName) {
        return "SELECT * FROM " + tableName + " where 1=0";
    }

    @Override
    public String queryFieldComment(String schemaName, String tableName, String columnName) {
        return null;
    }

    @Override
    public String queryPrimaryKey() {
        return null;
    }

    @Override
    public String queryTableNameComment() {
        return "select table_name,table_comment from information_schema.tables where table_schema=? and table_name = ?";
    }

    @Override
    public String queryTablesNameComments() {
        return "select table_name,table_comment from information_schema.tables where table_schema=?";
    }

    @Override
    public String queryTables() {
        return "show tables";
    }

    @Override
    public String queryTableTotal(String schemaName, String tableName) {
        return null;
    }

    @Override
    public String pageQueryTables(String schemaName, String tableName, int currentPage, int pageSize) {
        return null;
    }

    @Override
    public String queryTables(String tableSchema) {
        return null;
    }

    @Override
    public String queryDatabases() {
        return "show databases";
    }

    @Override
    public String queryTableSchema(String... args) {
        return null;
    }

    @Override
    public String queryDataByLimit(String schemaName, String tableName, int limit) {
        return null;
    }

    @Override
    public String queryMaxId(String tableName, String primaryKey) {
        return String.format("select max(%s) from %s",primaryKey,tableName);
    }
}
