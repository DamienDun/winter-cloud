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

    /** JDBC 数据源 ID */
    @NotNull
    @Schema(description = "数据源ID")
    private Long datasourceId;

    /** 待删除表名 */
    @NotBlank
    @Schema(description = "表名")
    private String tableName;

    /** 确认表名，必须与 tableName 完全一致，防止误删 */
    @NotBlank
    @Schema(description = "确认表名（须与表名一致）")
    private String confirmTableName;
}
