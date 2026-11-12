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
     * 执行 API 并解析为行数据（非分页接口只调一次；分页接口仅用于兼容，仍会全量累加，作业同步请用 {@link #forEachDataPage}）。
     */
    List<Map<String, Object>> fetchDataRows(Long id);

    /**
     * 按页拉取：每页回调一次，不在内存累加全部结果。
     * 非分页接口只回调第 1 页；分页接口从第 1 页翻到业务数组为空。
     *
     * @return 总行数
     */
    int forEachDataPage(Long id, ApiPageConsumer pageConsumer);

    @FunctionalInterface
    interface ApiPageConsumer {
        /**
         * @param pageNum 页码（从 1 起）
         * @param rows    本页扁平行，可为空表示结束
         */
        void accept(int pageNum, List<Map<String, Object>> rows) throws Exception;
    }
    int insert(DsApiDatasource entity);
    int update(DsApiDatasource entity);
    /** 批量修改状态：0 停用，1 启用 */
    int changeStatus(Long[] ids, Integer status);
    int deleteByIds(Long[] ids);
    DsApiTestResult testConnection(DsApiDatasource entity);
}
