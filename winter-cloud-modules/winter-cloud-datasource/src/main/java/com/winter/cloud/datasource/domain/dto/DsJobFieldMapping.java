package com.winter.cloud.datasource.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 作业字段映射：源字段 -> 目标字段（列表顺序即同步建表/补列顺序）
 */
@Data
@Schema(description = "作业字段映射项")
public class DsJobFieldMapping {

    /** 源字段名或路径（JDBC 列名；API 为 JSON/XML 点路径） */
    @Schema(description = "源字段")
    private String sourceField;

    /** 目标表字段名 */
    @Schema(description = "目标字段")
    private String targetField;

    /**
     * 目标列类型，如 varchar(64)、bigint、datetime。
     * 空：API 源默认按样例推断；JDBC 源沿用源列类型。
     */
    @Schema(description = "目标字段类型（可空）")
    private String targetType;

    /**
     * 目标列注释。
     * 空：JDBC 沿用源注释；API 可用源字段路径。
     */
    @Schema(description = "目标字段注释（可空）")
    private String targetComment;

    /**
     * 是否作为目标表主键（可多列组成复合主键）。
     * null：兼容旧配置，JDBC 源可沿用源列主键；true/false：以配置为准。
     */
    @Schema(description = "是否主键（可空，兼容旧数据）")
    private Boolean primaryKey;
}
