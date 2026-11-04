package com.winter.cloud.common.datasource.sqlbuilder;

import com.alibaba.druid.util.JdbcConstants;

/**
 * ClickHouse builder
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2023/4/20 15:50
 */
public class ClickHouseSqlBuilder extends AbstractSqlBuidler implements SqlBuilder {

    private volatile static ClickHouseSqlBuilder single;

    public static ClickHouseSqlBuilder getInstance() {
        if (single == null) {
            synchronized (ClickHouseSqlBuilder.class) {
                if (single == null) {
                    single = new ClickHouseSqlBuilder();
                }
            }
        }
        return single;
    }

    @Override
    public String getDatasourceType() {
        return JdbcConstants.CLICKHOUSE.name();
    }
}
