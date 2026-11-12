package com.winter.cloud.datasource.domain.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * API 数据源依赖项（有序；别名用于占位符 {{rely.别名.字段}}）
 */
@Data
@Schema(description = "API依赖项")
public class DsApiRelyItem {

    /** 别名，如 auth / user；同数据源内唯一 */
    @Schema(description = "依赖别名")
    private String alias;

    /** 依赖的 API 数据源 ID */
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "依赖API数据源ID")
    private Long datasourceId;

    /** 执行顺序，越小越先；同级后项可引用先项出参 */
    @Schema(description = "执行顺序")
    private Integer sort;
}
