package com.winter.cloud.datasource.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 列定义（建表/改字段）
 */
@Getter
@Setter
@NoArgsConstructor
@Schema(description = "列定义")
public class DsColumnDef {

    @Schema(description = "列名")
    private String name;

    /** 如 varchar(64)、bigint、decimal(10,2) */
    @Schema(description = "字段类型")
    private String type;

    @Schema(description = "是否可空")
    private Boolean nullable = true;

    @Schema(description = "是否主键")
    private Boolean primaryKey = false;

    @Schema(description = "默认值（原样写入，不加引号时请自行包含）")
    private String defaultValue;

    @Schema(description = "列注释")
    private String comment;

    /** 修改字段时：原列名（为空表示与 name 相同） */
    @Schema(description = "原列名（改名时用）")
    private String oldName;
}