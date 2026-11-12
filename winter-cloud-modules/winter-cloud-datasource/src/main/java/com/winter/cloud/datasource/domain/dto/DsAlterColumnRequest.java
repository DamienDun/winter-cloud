package com.winter.cloud.datasource.domain.dto;

import com.winter.cloud.common.datasource.ddl.model.DsColumnDef;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 增/改/删列请求
 */
@Getter
@Setter
@NoArgsConstructor
@Schema(description = "改字段请求")
public class DsAlterColumnRequest {

    /** JDBC 数据源 ID */
    @NotNull
    @Schema(description = "数据源ID")
    private Long datasourceId;

    /** 目标表名 */
    @NotBlank
    @Schema(description = "表名")
    private String tableName;

    /** 操作类型：ADD 新增列 / MODIFY 修改列 / DROP 删除列 */
    @NotBlank
    @Schema(description = "操作类型：ADD/MODIFY/DROP")
    private String action;

    /** 列定义：DROP 时只需 name；ADD/MODIFY 填完整列定义 */
    @NotNull
    @Schema(description = "列定义")
    private DsColumnDef column;
}
