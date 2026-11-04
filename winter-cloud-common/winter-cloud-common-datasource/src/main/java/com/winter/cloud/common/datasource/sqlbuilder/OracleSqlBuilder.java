package com.winter.cloud.common.datasource.sqlbuilder;

import com.alibaba.druid.util.JdbcConstants;
import com.winter.cloud.common.core.utils.StringUtils;

/**
 * oracle sqlBuilder
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2023/4/20 13:54
 */
public class OracleSqlBuilder extends AbstractSqlBuidler implements SqlBuilder {

    private volatile static OracleSqlBuilder single;

    public static OracleSqlBuilder getInstance() {
        if (single == null) {
            synchronized (OracleSqlBuilder.class) {
                if (single == null) {
                    single = new OracleSqlBuilder();
                }
            }
        }
        return single;
    }

    @Override
    public String getDatasourceType() {
        return JdbcConstants.ORACLE.name();
    }

    @Override
    public String queryFieldComment(String schemaName, String tableName, String columnName) {
        return String.format("select B.comments \n" +
                "  from user_tab_columns A, user_col_comments B\n" +
                " where a.COLUMN_NAME = b.column_name\n" +
                "   and A.Table_Name = B.Table_Name\n" +
                "   and A.Table_Name = upper('%s')\n" +
                "   AND A.column_name  = '%s'", tableName, columnName);
    }

    @Override
    public String queryPrimaryKey() {
        return "select cu.column_name from user_cons_columns cu, user_constraints au where cu.constraint_name = au.constraint_name and au.owner = ? and au.constraint_type = 'P' and au.table_name = ?";
    }

    @Override
    public String queryTableNameComment() {
        return "select table_name,comments from user_tab_comments where table_name = ?";
    }

    @Override
    public String queryTablesNameComments() {
        return "select table_name,comments from user_tab_comments";
    }

    @Override
    public String queryTables() {
        return "select table_name from user_tab_comments";
    }

    @Override
    public String queryTables(String tableSchema) {
        return "select table_name from dba_tables where owner='" + tableSchema + "'";
    }

    @Override
    public String queryTableTotal(String schemaName, String tableName) {
        String tableNameSql = StringUtils.isNotEmpty(tableName) ? " where table_name like '%" + tableName + "%' " : "";
        return String.format("select count(*) from user_tables %s ", tableNameSql);
    }

    @Override
    public String pageQueryTables(String schemaName, String tableName, int currentPage, int pageSize) {
        String tableNameSql = StringUtils.isNotEmpty(tableName) ? " and table_name like '%" + tableName + "%' " : "";
        return String.format("SELECT table_name FROM (select table_name, ROWNUM rn from user_tables where 1=1 %s ) WHERE rn BETWEEN %d AND %d ",
                tableNameSql, ((currentPage - 1) * pageSize + 1), currentPage * pageSize);
    }

    @Override
    public String queryTableSchema(String... args) {
        return "select username from sys.dba_users";
    }

    @Override
    public String queryDataByLimit(String schemaName, String tableName, int limit) {
        return String.format("SELECT * FROM %s where ROWNUM < %d", tableName, limit);
    }
}
