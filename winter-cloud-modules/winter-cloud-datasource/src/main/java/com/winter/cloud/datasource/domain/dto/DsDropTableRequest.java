package com.winter.cloud.datasource.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 删表请求（需二次确认表名）
 */
@Getter
@Setter
@NoArgsConstructor
@Schema(description = "删表请求")
public class DsDropTableRequest {

    @NotNull
    private Long datasourceId;

    @NotBlank
    private String tableName;

    /** 必须与 tableName 完全一致，防止误删 */
    @NotBlank
    private String confirmTableName;
}