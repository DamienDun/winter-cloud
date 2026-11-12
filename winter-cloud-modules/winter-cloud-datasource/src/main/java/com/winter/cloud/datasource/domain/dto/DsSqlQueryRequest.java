package com.winter.cloud.datasource.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 自定义 SQL 查询请求（只读）
 */
@Getter
@Setter
@NoArgsConstructor
@Schema(description = "SQL查询请求")
public class DsSqlQueryRequest {

    /** JDBC 数据源 ID */
    @NotNull(message = "数据源ID不能为空")
    @Schema(description = "数据源ID")
    private Long datasourceId;

    /** SQL 语句（仅允许 SELECT/WITH/SHOW/DESC/EXPLAIN） */
    @NotBlank(message = "SQL不能为空")
    @Schema(description = "SQL语句（SELECT/WITH/SHOW/DESC/EXPLAIN）")
    private String sql;

    /** 最大返回行数，默认 200，硬上限 1000 */
    @Schema(description = "最大返回行数（默认200，上限1000）")
    private Integer maxRows;
}
