package com.winter.cloud.datasource.support;

import com.winter.cloud.common.datasource.info.DatasourceInfo;
import com.winter.cloud.common.datasource.query.AbstractDsQueryTool;
import com.winter.cloud.common.datasource.query.DsQueryTool;
import com.winter.cloud.common.datasource.query.DsQueryToolFactory;
import com.winter.cloud.common.datasource.util.AESUtil;
import com.winter.cloud.common.datasource.util.JdbcIdentUtils;
import com.winter.cloud.datasource.domain.entity.DsJdbcDatasource;
import com.winter.cloud.datasource.mapper.DsJdbcDatasourceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 按已保存数据源打开 / 关闭查询工具（依赖本模块 Mapper/实体，留在业务模块）
 */
@Component
public class DsQueryToolSupport {

    @Autowired
    private DsJdbcDatasourceMapper datasourceMapper;

    /**
     * 打开连接（调用方必须在 finally 中 {@link #close(DsQueryTool)}）
     */
    public AbstractDsQueryTool open(Long datasourceId) {
        if (datasourceId == null) {
            throw new IllegalArgumentException("数据源 ID 不能为空");
        }
        DsJdbcDatasource entity = datasourceMapper.selectById(datasourceId);
        if (entity == null) {
            throw new IllegalArgumentException("数据源不存在");
        }
        if (entity.getStatus() != null && entity.getStatus() == 0) {
            throw new IllegalArgumentException("数据源已停用");
        }
        DatasourceInfo info = toDatasourceInfo(entity);
        boolean looksEncrypted = AESUtil.decrypt(info.getJdbcPassword()) != null;
        info.setAesEncryptPassword(looksEncrypted);
        // 管理端短连接：不走池缓存，用完即关
        info.setDisableCache(true);
        try {
            return (AbstractDsQueryTool) DsQueryToolFactory.getByDbType(info);
        } catch (Exception e) {
            throw new IllegalStateException("打开数据源失败：" + e.getMessage(), e);
        }
    }

    public void close(DsQueryTool tool) {
        if (tool instanceof AbstractDsQueryTool) {
            ((AbstractDsQueryTool) tool).close();
        }
    }

    public DatasourceInfo toDatasourceInfo(DsJdbcDatasource entity) {
        DatasourceInfo info = new DatasourceInfo();
        info.setKey("ds:" + (entity.getId() != null ? entity.getId() : System.nanoTime()));
        info.setDatasourceName(entity.getDatasourceName());
        info.setDatasourceType(entity.getDatasourceType());
        info.setDatabase(entity.getDatabaseName());
        info.setJdbcUsername(entity.getJdbcUsername());
        info.setJdbcPassword(entity.getJdbcPassword());
        info.setJdbcUrl(entity.getJdbcUrl());
        info.setJdbcDriverClass(entity.getJdbcDriverClass());
        return info;
    }

    /** @see JdbcIdentUtils#quoteIdent(String, String) */
    public static String quoteIdent(String datasourceType, String name) {
        return JdbcIdentUtils.quoteIdent(datasourceType, name);
    }
}
