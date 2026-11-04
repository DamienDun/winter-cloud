package com.winter.cloud.datasource.service;

import com.winter.cloud.common.datasource.info.ColumnInfo;
import com.winter.cloud.datasource.domain.dto.DsTableRowRequest;

import java.util.List;
import java.util.Map;

/**
 * 远程数据源表数据 CRUD
 */
public interface IDsTableDataService {

    List<String> listTables(Long datasourceId, String tableName);

    List<ColumnInfo> listColumns(Long datasourceId, String tableName);

    /**
     * @return total + rows
     */
    Map<String, Object> pageRows(Long datasourceId, String tableName, int pageNum, int pageSize);

    int insertRow(DsTableRowRequest request);

    int updateRow(DsTableRowRequest request);

    int deleteRow(DsTableRowRequest request);
}
