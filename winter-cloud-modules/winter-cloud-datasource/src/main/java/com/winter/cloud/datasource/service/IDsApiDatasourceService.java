package com.winter.cloud.datasource.service;

import com.winter.cloud.datasource.domain.dto.DsApiFieldItem;
import com.winter.cloud.datasource.domain.dto.DsApiTestResult;
import com.winter.cloud.datasource.domain.entity.DsApiDatasource;

import java.util.List;
import java.util.Map;

public interface IDsApiDatasourceService {
    List<DsApiDatasource> selectList(DsApiDatasource query);
    DsApiDatasource selectById(Long id);
    /** other enabled API datasources; name fuzzy match */
    List<DsApiDatasource> selectRelyCandidates(Long excludeId, String datasourceName);
    /** parse response field paths from a datasource by testing connection */
    List<DsApiFieldItem> listOutputFields(Long id);
    /**
     * 执行 API 并解析为行数据。每行 Map 的 key 同时包含相对字段与带容器前缀路径（如 id / data.id），
     * 便于与作业字段映射中的 sourceField 对齐。
     */
    List<Map<String, Object>> fetchDataRows(Long id);
    int insert(DsApiDatasource entity);
    int update(DsApiDatasource entity);
    int deleteByIds(Long[] ids);
    DsApiTestResult testConnection(DsApiDatasource entity);
}
