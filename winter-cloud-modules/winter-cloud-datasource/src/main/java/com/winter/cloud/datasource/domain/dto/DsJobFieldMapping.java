package com.winter.cloud.datasource.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 作业字段映射：源字段 -> 目标字段（含可选类型/注释；列表顺序即同步建表顺序）
 */
@Data
@Schema(description = "作业字段映射项")
public class DsJobFieldMapping {

    @Schema(description = "源字段")
    private String sourceField;

    @Schema(description = "目标字段")
    private String targetField;

    /**
     * 目标列类型，如 varchar(64)、bigint、datetime。
     * 空：API 源默认 varchar(255)；JDBC 源沿用源列类型。
     */
    @Schema(description = "目标字段类型（可空）")
    private String targetType;

    /**
     * 目标列注释。空则 JDBC 沿用源注释，API 可用源字段路径。
     */
    @Schema(description = "目标字段注释（可空）")
    private String targetComment;
}
