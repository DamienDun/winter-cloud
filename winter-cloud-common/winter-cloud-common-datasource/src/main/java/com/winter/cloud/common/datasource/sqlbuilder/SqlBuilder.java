package com.winter.cloud.common.datasource.sqlbuilder;

/**
 * sql builder 接口
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2023/4/20 13:48
 */
public interface SqlBuilder {

    String getDatasourceType();

    /**
     * 查询字段列表
     *
     * @param tableName 表名
     * @return
     */
    String queryFields(String tableName);

    /**
     * 查询字段注释
     *
     * @param schemaName schema
     * @param tableName  表名
     * @param columnName 字段名
     * @return
     */
    String queryFieldComment(String schemaName, String tableName, String columnName);

    /**
     * 查询主键字段
     *
     * @return
     */
    String queryPrimaryKey();

    /**
     * 查询单个表注释
     *
     * @return
     */
    String queryTableNameComment();

    /**
     * 查询某个库下的所有表注释
     *
     * @return
     */
    String queryTablesNameComments();

    /**
     * 获取所有表名的sql
     *
     * @return
     */
    String queryTables();

    /**
     * 条件查询表总数量
     *
     * @param schemaName
     * @param tableName
     * @return
     */
    String queryTableTotal(String schemaName, String tableName);

    /**
     * 分页查询所有表名
     *
     * @param schemaName
     * @param tableName
     * @param currentPage
     * @param pageSize
     * @return
     */
    String pageQueryTables(String schemaName, String tableName, int currentPage, int pageSize);

    /**
     * 获取所有表名的sql
     *
     * @param tableSchema
     * @return
     */
    String queryTables(String tableSchema);

    /**
     * 获取所有库
     *
     * @return
     */
    String queryDatabases();

    /**
     * 获取 Table schema
     *
     * @return
     */
    String queryTableSchema(String... args);

    /**
     * 获取前limit条数据
     *
     * @param schemaName
     * @param tableName
     * @param limit
     * @return
     */
    String queryDataByLimit(String schemaName, String tableName, int limit);

    /**
     * 获取当前表maxId
     *
     * @param tableName  表名
     * @param primaryKey 主键
     * @return
     */
    String queryMaxId(String tableName, String primaryKey);

}
