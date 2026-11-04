package com.winter.cloud.common.datasource.query;

import com.winter.cloud.common.datasource.info.DatasourceInfo;

import java.sql.SQLException;

/**
 * hive 查询工具
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2023/4/20 14:43
 */
public class HiveQueryTool extends AbstractDsQueryTool implements DsQueryTool {

    public HiveQueryTool(DatasourceInfo datasourceInfo) throws SQLException {
        super(datasourceInfo);
    }

}

