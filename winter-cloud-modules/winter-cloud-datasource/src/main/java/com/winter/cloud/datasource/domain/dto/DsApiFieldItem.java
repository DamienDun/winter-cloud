package com.winter.cloud.datasource.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API 响应出参字段（用于作业字段映射候选）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "API出参字段")
public class DsApiFieldItem {

    /** 字段路径，如 access_token、data.id、Envelope.Body.return.id */
    @Schema(description = "字段路径")
    private String fieldName;

    /** 样例值（取自一次真实响应，用于展示与类型推断） */
    @Schema(description = "样例值")
    private String sampleValue;

    /** 根据样例值推断的目标 SQL 类型，如 bigint、varchar(255) */
    @Schema(description = "推断SQL类型")
    private String inferredType;

    public DsApiFieldItem(String fieldName, String sampleValue) {
        this.fieldName = fieldName;
        this.sampleValue = sampleValue;
        this.inferredType = null;
    }
}
