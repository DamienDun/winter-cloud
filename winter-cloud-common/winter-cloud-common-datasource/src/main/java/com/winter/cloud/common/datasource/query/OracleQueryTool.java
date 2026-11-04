package com.winter.cloud.common.datasource.query;

import com.winter.cloud.common.datasource.info.DatasourceInfo;

import java.sql.SQLException;

/**
 * Oracle 查询工具
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2023/4/20 14:44
 */
public class OracleQueryTool extends AbstractDsQueryTool implements DsQueryTool {

    public OracleQueryTool(DatasourceInfo datasourceInfo) throws SQLException {
        super(datasourceInfo);
    }
}
