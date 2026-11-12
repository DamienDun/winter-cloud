package com.winter.cloud.datasource.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "API response field")
public class DsApiFieldItem {
    /** dotted path, e.g. access_token / data.token */
    private String fieldName;
    private String sampleValue;
    /** 根据样例值推断的目标 SQL 类型，如 bigint / varchar(255) */
    private String inferredType;

    public DsApiFieldItem(String fieldName, String sampleValue) {
        this.fieldName = fieldName;
        this.sampleValue = sampleValue;
        this.inferredType = null;
    }
}
