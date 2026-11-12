package com.winter.cloud.datasource.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 批量修改状态请求（JDBC / API 数据源共用）
 */
@Data
@Schema(description = "批量修改状态请求")
public class DsChangeStatusRequest {

    /** 主键列表（字符串，避免前端雪花精度丢失） */
    @Schema(description = "主键ID列表")
    private List<String> ids;

    /** 目标状态：0 停用，1 启用 */
    @Schema(description = "状态：0停用，1启用")
    private Integer status;
}
