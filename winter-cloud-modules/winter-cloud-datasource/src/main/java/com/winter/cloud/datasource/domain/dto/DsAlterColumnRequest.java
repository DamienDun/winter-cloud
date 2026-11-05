package com.winter.cloud.datasource.domain.dto;

import com.winter.cloud.common.datasource.ddl.model.DsColumnDef;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 增/改/删列
 */
@Getter
@Setter
@NoArgsConstructor
@Schema(description = "改字段请求")
public class DsAlterColumnRequest {

    @NotNull
    private Long datasourceId;

    @NotBlank
    private String tableName;

    /** ADD / MODIFY / DROP */
    @NotBlank
    private String action;

    /** DROP 时只需 name；ADD/MODIFY 填完整列定义 */
    @NotNull
    private DsColumnDef column;
}