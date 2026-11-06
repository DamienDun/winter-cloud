package com.winter.cloud.datasource.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "API connection test result")
public class DsApiTestResult {
    /** 0 success, 1 fail */
    private Integer testStatus;
    private Long testTime;
    private String testResult;
}
