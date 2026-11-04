package com.winter.cloud.common.datasource.sqlbuilder;

import com.alibaba.druid.util.JdbcConstants;

import java.util.stream.Stream;

/**
 * sql builder factory
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2023/4/20 14:06
 */
public enum SqlBuilderFactory {

    SQL_SERVER(JdbcConstants.SQL_SERVER.name(), SQLServerSqlBuilder.getInstance()),
    ORACLE(JdbcConstants.ORACLE.name(), OracleSqlBuilder.getInstance()),
    POSTGRESQL(JdbcConstants.POSTGRESQL.name(), PostgreSQLSqlBuilder.getInstance()),
    PRESTO(JdbcConstants.PRESTO, PrestoSqlBuilder.getInstance()),
    HIVE(JdbcConstants.HIVE.name(), HiveSqlBuilder.getInstance()),
    MYSQL(JdbcConstants.MYSQL.name(), MySQLSqlBuilder.getInstance()),
    ;
    /**
     * 数据库类型
     */
    private String dbType;

    private SqlBuilder sqlBuilder;

    public String getDbType() {
        return dbType;
    }

    public SqlBuilder getSqlBuilder() {
        return sqlBuilder;
    }

    SqlBuilderFactory(String dbType, SqlBuilder sqlBuilder) {
        this.dbType = dbType;
        this.sqlBuilder = sqlBuilder;
    }

    public static SqlBuilder getByDbType(String dbType) {
        return Stream.of(SqlBuilderFactory.values()).filter(x -> x.getDbType().equalsIgnoreCase(dbType)).findAny()
                .orElseThrow(() -> new RuntimeException("unknown database type error")).getSqlBuilder();
    }
}
