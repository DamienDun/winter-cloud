package com.winter.cloud.datasource.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * API 连接测试结果
 */
@Data
@Schema(description = "API连接测试结果")
public class DsApiTestResult {

    /** 测试状态：0 成功，1 失败 */
    @Schema(description = "测试状态：0成功，1失败")
    private Integer testStatus;

    /** 测试耗时（毫秒） */
    @Schema(description = "测试耗时（毫秒）")
    private Long testTime;

    /** 测试结果正文（成功多为响应原文；失败为错误说明） */
    @Schema(description = "测试结果正文")
    private String testResult;
}
