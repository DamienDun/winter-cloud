package com.winter.cloud.common.datasource.ddl;

import java.util.Arrays;
import java.util.List;

/**
 * Resolve DDL dialect by datasource type
 */
public final class DsDdlDialectFactory {

    private static final List<DsDdlDialect> DIALECTS = Arrays.asList(
            new MysqlDdlDialect(),
            new PostgresqlDdlDialect(),
            new OracleDdlDialect(),
            new SqlServerDdlDialect(),
            new HiveDdlDialect(),
            new ClickHouseDdlDialect(),
            new PrestoDdlDialect()
    );

    private DsDdlDialectFactory() {
    }

    public static DsDdlDialect get(String datasourceType) {
        for (DsDdlDialect d : DIALECTS) {
            if (d.supports(datasourceType)) {
                return d;
            }
        }
        throw new IllegalArgumentException("\u8868\u7ed3\u6784 DDL \u6682\u4e0d\u652f\u6301\u8be5\u6570\u636e\u6e90\u7c7b\u578b\uff1a" + datasourceType
                + "\uff08\u5f53\u524d\u652f\u6301\uff1aMySQL/MariaDB\u3001PostgreSQL\u3001Oracle\u3001SQL Server\u3001Hive\u3001ClickHouse\u3001Presto/Trino\uff09");
    }
}