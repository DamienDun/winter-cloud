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
}
