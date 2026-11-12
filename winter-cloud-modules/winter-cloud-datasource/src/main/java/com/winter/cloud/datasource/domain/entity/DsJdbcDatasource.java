package com.winter.cloud.datasource.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.winter.cloud.common.mybatis.domain.entity.DefaultBaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JDBC 数据源配置
 */
@Getter
@Setter
@NoArgsConstructor
@TableName("ds_jdbc_datasource")
@Schema(description = "JDBC数据源")
public class DsJdbcDatasource extends DefaultBaseEntity {

    private static final long serialVersionUID = 1L;

    /** 数据源名称 */
    @Schema(description = "数据源名称")
    private String datasourceName;

    /** 数据源分组，默认 Default */
    @Schema(description = "数据源分组")
    private String datasourceGroup;

    /** 数据源类型：mysql/oracle/postgresql/sqlserver/hive/clickhouse/presto 等 */
    @Schema(description = "数据源类型：mysql/oracle/postgresql/sqlserver/hive/clickhouse/presto")
    private String datasourceType;

    /** JDBC 用户名 */
    @Schema(description = "JDBC用户名")
    private String jdbcUsername;

    /** JDBC 密码（入库前 AES 加密） */
    @Schema(description = "JDBC密码（入库AES加密）")
    private String jdbcPassword;

    /** JDBC 连接 URL */
    @Schema(description = "JDBC连接URL")
    private String jdbcUrl;

    /** JDBC 驱动类全限定名 */
    @Schema(description = "JDBC驱动类")
    private String jdbcDriverClass;

    /** 数据库名（可选，部分库型可从 URL 解析） */
    @Schema(description = "数据库名（可选）")
    private String databaseName;

    /** 状态：0 停用，1 启用 */
    @Schema(description = "状态：0停用，1启用")
    private Integer status;
}
