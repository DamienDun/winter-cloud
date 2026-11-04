package com.winter.cloud.common.datasource.query;

import com.winter.cloud.common.datasource.info.DatasourceInfo;

import java.sql.SQLException;

/**
 * ClickHouse查询工具
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2023/4/20 15:59
 */
public class ClickHouseQueryTool extends AbstractDsQueryTool implements DsQueryTool {

    public ClickHouseQueryTool(DatasourceInfo datasourceInfo) throws SQLException {
        super(datasourceInfo);
    }
}
