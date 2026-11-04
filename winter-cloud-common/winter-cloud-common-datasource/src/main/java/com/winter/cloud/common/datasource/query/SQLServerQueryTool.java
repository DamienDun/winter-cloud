package com.winter.cloud.common.datasource.query;

import com.winter.cloud.common.datasource.info.DatasourceInfo;

import java.sql.SQLException;

/**
 * SQL Server 查询工具
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2023/4/20 14:47
 */
public class SQLServerQueryTool extends AbstractDsQueryTool implements DsQueryTool {

    public SQLServerQueryTool(DatasourceInfo datasourceInfo) throws SQLException {
        super(datasourceInfo);
    }
}
