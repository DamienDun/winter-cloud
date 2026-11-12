package com.winter.cloud.datasource.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * API 请求参数单项（Header / Query / Page）
 */
@Data
@Schema(description = "API请求参数项")
public class DsApiReqParamsItem {

    /** 参数名 */
    @Schema(description = "参数名")
    private String name;

    /** 参数类型（如 string/number，前端展示用） */
    @Schema(description = "参数类型")
    private String type;

    /** 参数值（可含 {{rely.字段路径}} 占位符） */
    @Schema(description = "参数值")
    private String value;

    /** 是否启用：0 启用，1 停用 */
    @Schema(description = "是否启用：0启用，1停用")
    private Integer isAvailable;
}
