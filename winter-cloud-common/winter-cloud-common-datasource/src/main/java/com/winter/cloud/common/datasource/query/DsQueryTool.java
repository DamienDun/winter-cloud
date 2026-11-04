package com.winter.cloud.common.datasource.query;

import com.winter.cloud.common.datasource.info.ColumnInfo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;
import java.util.Map;

/**
 * 数据源基础查询工具
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2023/4/20 13:34
 */
public interface DsQueryTool {

    /**
     * 获取jdbcTemplate
     *
     * @return
     */
    JdbcTemplate getJdbcTemplate();

    /**
     * 验证表是否存在
     *
     * @param tableName 表名
     * @return
     */
    boolean tableExist(String tableName);

    /**
     * 获取所有可用表名
     *
     * @return
     */
    List<String> getTableNames();

    /**
     * 获取所有可用表名
     *
     * @return
     */
    List<String> getTableNames(String tableSchema);

    /**
     * 条件查询表总数量
     *
     * @param tableSchema
     * @param tableName
     * @return
     */
    int getTableTotal(String tableSchema, String tableName);


    /**
     * 分页查询表名
     *
     * @param schemaName
     * @param tableName
     * @param currentPage
     * @param pageSize
     * @return
     */
    List<String> pageQueryTableNames(String schemaName, String tableName, int currentPage, int pageSize);

    /**
     * 根据表名获取所有字段名
     *
     * @param tableName
     * @return
     */
    List<String> getColumnNames(String tableName);

    /**
     * 根据表名获取所有字段信息
     *
     * @param tableName
     * @return
     */
    List<ColumnInfo> getColumnInfos(String tableName);

    /**
     * 获取所有库名
     *
     * @return
     */
    List<String> getDatabaseNames();

    /**
     * 查询map list
     *
     * @param sql 查询的sql
     * @return 查询的数据
     */
    List<Map<String, Object>> queryMapList(String sql);

    /**
     * 查询map list 前limit条数据
     *
     * @param schemaName schema名
     * @param tableName  表名
     * @param limit      限制的条数
     * @return 查询的数据
     */
    List<Map<String, Object>> queryMapList(String schemaName, String tableName, int limit);

    /**
     * 查询 Object
     *
     * @param sql          查询sql
     * @param requiredType 期望返回的数据类型
     * @param <T>          泛型
     * @return 查询的数据
     */
    <T> T queryForObject(String sql, Class<T> requiredType);

    /**
     * 查询 Object 适用于对象
     *
     * @param sql       查询sql
     * @param rowMapper 行数据的映射
     * @param <T>       泛型
     * @return
     */
    <T> T queryForObject(String sql, RowMapper<T> rowMapper);

    /**
     * 查询 Object集合
     *
     * @param <T>          泛型
     * @param sql          查询sql
     * @param requiredType 期望返回的数据类型
     * @return 查询的数据
     */
    <T> List<T> queryForList(String sql, Class<T> requiredType);

    /**
     * 查询 Object集合 适用于对象集合
     *
     * @param sql       查询sql
     * @param rowMapper 行数据的映射
     * @param <T>       泛型
     * @return
     */
    <T> List<T> queryForList(String sql, RowMapper<T> rowMapper);

    /**
     * 获取当前表maxId
     *
     * @param tableName
     * @param primaryKey
     * @return
     */
    long queryMaxIdVal(String tableName, String primaryKey);
}
