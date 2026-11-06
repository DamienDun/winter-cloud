package com.winter.cloud.datasource.service;

import com.winter.cloud.datasource.domain.dto.DsApiFieldItem;
import com.winter.cloud.datasource.domain.dto.DsApiTestResult;
import com.winter.cloud.datasource.domain.entity.DsApiDatasource;

import java.util.List;

public interface IDsApiDatasourceService {
    List<DsApiDatasource> selectList(DsApiDatasource query);
    DsApiDatasource selectById(Long id);
    /** other enabled API datasources; name fuzzy match */
    List<DsApiDatasource> selectRelyCandidates(Long excludeId, String datasourceName);
    /** parse response field paths from a datasource by testing connection */
    List<DsApiFieldItem> listOutputFields(Long id);
    int insert(DsApiDatasource entity);
    int update(DsApiDatasource entity);
    int deleteByIds(Long[] ids);
    DsApiTestResult testConnection(DsApiDatasource entity);
}
