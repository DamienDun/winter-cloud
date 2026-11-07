package com.winter.cloud.datasource.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 作业字段映射：源字段 -> 目标字段
 */
@Data
@Schema(description = "作业字段映射项")
public class DsJobFieldMapping {

    @Schema(description = "源字段")
    private String sourceField;

    @Schema(description = "目标字段")
    private String targetField;
}
