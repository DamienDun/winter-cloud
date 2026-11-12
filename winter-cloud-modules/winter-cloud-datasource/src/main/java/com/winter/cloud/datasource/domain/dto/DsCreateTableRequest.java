package com.winter.cloud.datasource.domain.dto;

import com.winter.cloud.common.datasource.ddl.model.DsColumnDef;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 建表请求
 */
@Getter
@Setter
@NoArgsConstructor
@Schema(description = "建表请求")
public class DsCreateTableRequest {

    /** JDBC 数据源 ID */
    @NotNull
    @Schema(description = "数据源ID")
    private Long datasourceId;

    /** 表名 */
    @NotBlank
    @Schema(description = "表名")
    private String tableName;

    /** 表注释（可空） */
    @Schema(description = "表注释")
    private String comment;

    /** 列定义列表（至少一列） */
    @NotEmpty
    @Schema(description = "列定义列表")
    private List<DsColumnDef> columns;
}
