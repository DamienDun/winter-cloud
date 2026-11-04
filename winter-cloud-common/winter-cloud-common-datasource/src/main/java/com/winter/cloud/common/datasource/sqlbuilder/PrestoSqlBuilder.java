package com.winter.cloud.common.datasource.sqlbuilder;

import com.alibaba.druid.util.JdbcConstants;

/**
 * presto sqlbuilder
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2023/4/20 13:55
 */
public class PrestoSqlBuilder extends AbstractSqlBuidler implements SqlBuilder {

    private volatile static PrestoSqlBuilder single;

    public static PrestoSqlBuilder getInstance() {
        if (single == null) {
            synchronized (PrestoSqlBuilder.class) {
                if (single == null) {
                    single = new PrestoSqlBuilder();
                }
            }
        }
        return single;
    }

    @Override
    public String getDatasourceType() {
        return JdbcConstants.PRESTO;
    }
}
