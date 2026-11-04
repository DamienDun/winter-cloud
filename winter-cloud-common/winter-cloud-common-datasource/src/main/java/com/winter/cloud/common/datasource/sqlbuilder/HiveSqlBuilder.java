package com.winter.cloud.common.datasource.sqlbuilder;

import com.alibaba.druid.util.JdbcConstants;

/**
 * Hive sql builder
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2023/4/20 13:53
 */
public class HiveSqlBuilder extends AbstractSqlBuidler implements SqlBuilder {

    private volatile static HiveSqlBuilder single;

    public static HiveSqlBuilder getInstance() {
        if (single == null) {
            synchronized (HiveSqlBuilder.class) {
                if (single == null) {
                    single = new HiveSqlBuilder();
                }
            }
        }
        return single;
    }

    @Override
    public String getDatasourceType() {
        return JdbcConstants.HIVE.name();
    }

}
