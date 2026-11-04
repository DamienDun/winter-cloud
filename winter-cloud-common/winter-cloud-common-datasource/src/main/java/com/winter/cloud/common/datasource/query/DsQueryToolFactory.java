package com.winter.cloud.common.datasource.query;

import com.alibaba.druid.util.JdbcConstants;
import com.winter.cloud.common.datasource.enums.DsResultEnum;
import com.winter.cloud.common.datasource.exception.DatasourceTypeException;
import com.winter.cloud.common.datasource.info.DatasourceInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 数据源查询工具工厂
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2023/4/20 15:10
 */
public class DsQueryToolFactory implements JdbcConstants {

    protected static final Logger logger = LoggerFactory.getLogger(DsQueryToolFactory.class);

    public static DsQueryTool getByDbType(DatasourceInfo datasourceInfo) {
        try {
            if (SQL_SERVER.name().equals(datasourceInfo.getDatasourceType())) {
                return new SQLServerQueryTool(datasourceInfo);
            }
            if (ORACLE.name().equals(datasourceInfo.getDatasourceType())) {
                return new OracleQueryTool(datasourceInfo);
            }
            if (POSTGRESQL.name().equals(datasourceInfo.getDatasourceType())) {
                return new PostgreSQLQueryTool(datasourceInfo);
            }
            if (MYSQL.name().equals(datasourceInfo.getDatasourceType())) {
                return new MySqlQueryTool(datasourceInfo);
            }
            if (PRESTO.equals(datasourceInfo.getDatasourceType())) {
                return new PrestoQueryTool(datasourceInfo);
            }
            if (HIVE.name().equals(datasourceInfo.getDatasourceType())) {
                return new HiveQueryTool(datasourceInfo);
            }
            if (CLICKHOUSE.name().equals(datasourceInfo.getDatasourceType())) {
                return new ClickHouseQueryTool(datasourceInfo);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new DatasourceTypeException(DsResultEnum.DS_CONN_DB_ERROR, e.getMessage());
        }
        throw new DatasourceTypeException(DsResultEnum.DS_TYPE_NOT_EXIST);
    }
}
