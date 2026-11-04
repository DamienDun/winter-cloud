package com.winter.cloud.datasource.support;

import com.winter.cloud.common.core.utils.StringUtils;
import com.winter.cloud.common.datasource.info.DatasourceInfo;
import com.winter.cloud.common.datasource.query.AbstractDsQueryTool;
import com.winter.cloud.common.datasource.query.DsQueryTool;
import com.winter.cloud.common.datasource.query.DsQueryToolFactory;
import com.winter.cloud.common.datasource.util.AESUtil;
import com.winter.cloud.datasource.domain.entity.DsJdbcDatasource;
import com.winter.cloud.datasource.mapper.DsJdbcDatasourceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 按已保存数据源打开 / 关闭查询工具
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

    /**
     * 校验并引用表名/列名，防止 SQL 注入
     */
    public static String quoteIdent(String datasourceType, String name) {
        if (StringUtils.isEmpty(name)) {
            throw new IllegalArgumentException("标识符不能为空");
        }
        String n = name.trim();
        // 仅允许常见标识符字符（含 $ #，兼容 Oracle）
        for (int i = 0; i < n.length(); i++) {
            char c = n.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9') || c == '_' || c == '$' || c == '#';
            if (!ok) {
                throw new IllegalArgumentException("非法标识符：" + name);
            }
        }
        String type = datasourceType == null ? "" : datasourceType.toLowerCase();
        if (type.contains("mysql") || type.contains("hive") || type.contains("clickhouse") || type.contains("mariadb")) {
            return "`" + n.replace("`", "``") + "`";
        }
        if (type.contains("sqlserver")) {
            return "[" + n.replace("]", "]]") + "]";
        }
        return "\"" + n.replace("\"", "\"\"") + "\"";
    }
}
