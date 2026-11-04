package com.winter.cloud.datasource.service;

import com.winter.cloud.datasource.domain.entity.DsJdbcDatasource;

import java.util.List;

/**
 * 数据源管理
 */
public interface IDsJdbcDatasourceService {

    List<DsJdbcDatasource> selectList(DsJdbcDatasource query);

    DsJdbcDatasource selectById(Long id);

    List<DsJdbcDatasource> selectAllEnabled();

    int insert(DsJdbcDatasource entity);

    int update(DsJdbcDatasource entity);

    int deleteByIds(Long[] ids);

    /**
     * 测试连接（可传未落库表单，也可传已有 id）
     */
    boolean testConnection(DsJdbcDatasource entity);
}
