package com.winter.cloud.datasource.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.winter.cloud.common.mybatis.domain.entity.DefaultBaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JDBC 数据源配置（表前缀 ds_）
 */
@Getter
@Setter
@NoArgsConstructor
@TableName("ds_jdbc_datasource")
@Schema(description = "JDBC数据源")
public class DsJdbcDatasource extends DefaultBaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "数据源名称")
    private String datasourceName;

    @Schema(description = "数据源分组")
    private String datasourceGroup;

    /** 类型：mysql/oracle/postgresql/sqlserver/hive/clickhouse/presto */
    @Schema(description = "数据源类型")
    private String datasourceType;

    @Schema(description = "JDBC 用户名")
    private String jdbcUsername;

    @Schema(description = "JDBC 密码（入库 AES 加密）")
    private String jdbcPassword;

    @Schema(description = "JDBC URL")
    private String jdbcUrl;

    @Schema(description = "JDBC 驱动类")
    private String jdbcDriverClass;

    @Schema(description = "数据库名（可选）")
    private String databaseName;

    /** 0停用 1启用 */
    @Schema(description = "状态：0停用 1启用")
    private Integer status;
}
