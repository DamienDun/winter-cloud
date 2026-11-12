package com.winter.cloud.datasource.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * 表行数据增删改请求
 */
@Getter
@Setter
@NoArgsConstructor
@Schema(description = "表行数据请求")
public class DsTableRowRequest {

    /** JDBC 数据源 ID */
    @NotNull(message = "数据源不能为空")
    @Schema(description = "数据源ID")
    private Long datasourceId;

    /** 表名 */
    @NotBlank(message = "表名不能为空")
    @Schema(description = "表名")
    private String tableName;

    /** 主键列名 -> 主键值（更新/删除必填） */
    @Schema(description = "主键条件")
    private Map<String, Object> pk;

    /** 行字段名 -> 字段值（新增/修改时提交） */
    @Schema(description = "行字段")
    private Map<String, Object> row;
}
