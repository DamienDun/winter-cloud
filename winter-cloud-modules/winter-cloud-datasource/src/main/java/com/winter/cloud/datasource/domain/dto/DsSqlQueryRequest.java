package com.winter.cloud.datasource.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * Custom SQL query request (readonly)
 */
@Getter
@Setter
@NoArgsConstructor
@Schema(description = "SQL query request")
public class DsSqlQueryRequest {

    @NotNull(message = "datasourceId required")
    private Long datasourceId;

    @NotBlank(message = "sql required")
    @Schema(description = "SQL (SELECT/WITH/SHOW/DESC/EXPLAIN)")
    private String sql;

    /** max rows, default 200, hard max 1000 */
    @Schema(description = "max rows")
    private Integer maxRows;
}