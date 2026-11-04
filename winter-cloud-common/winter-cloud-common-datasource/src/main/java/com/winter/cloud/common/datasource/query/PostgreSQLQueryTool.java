package com.winter.cloud.common.datasource.query;

import com.winter.cloud.common.datasource.info.DatasourceInfo;

import java.sql.SQLException;

/**
 * PostgreSQL 查询工具
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2023/4/20 14:45
 */
public class PostgreSQLQueryTool extends AbstractDsQueryTool implements DsQueryTool {

    public PostgreSQLQueryTool(DatasourceInfo datasourceInfo) throws SQLException {
        super(datasourceInfo);
    }
}
