package com.winter.cloud.datasource.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 创建/修改/删除索引（MySQL：名、字段、类型、方法、注释）
 */
@Getter
@Setter
@NoArgsConstructor
@Schema(description = "索引请求")
public class DsIndexRequest {

    @NotNull
    private Long datasourceId;

    @NotBlank
    private String tableName;

    @NotBlank
    private String indexName;

    /** 编辑时原索引名；为空表示新建 */
    @Schema(description = "原索引名（修改时必填）")
    private String oldIndexName;

    /**
     * 索引类型：NORMAL / UNIQUE / FULLTEXT / SPATIAL
     */
    @Schema(description = "索引类型")
    private String indexType = "NORMAL";

    /**
     * 索引方法：BTREE / HASH（FULLTEXT/SPATIAL 可空）
     */
    @Schema(description = "索引方法")
    private String indexMethod = "BTREE";

    @Schema(description = "索引注释")
    private String comment;

    /** 兼容旧字段：true 时等价 UNIQUE */
    private Boolean uniqueIndex = false;

    /** 创建/修改时必填 */
    private List<String> columns;
}
