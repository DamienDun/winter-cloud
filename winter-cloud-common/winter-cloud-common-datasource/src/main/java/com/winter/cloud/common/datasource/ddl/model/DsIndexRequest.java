package com.winter.cloud.common.datasource.ddl.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * Create / alter / drop index request
 */
@Getter
@Setter
@NoArgsConstructor
public class DsIndexRequest {

    @NotNull
    private Long datasourceId;

    @NotBlank
    private String tableName;

    @NotBlank
    private String indexName;

    /** original index name when editing */
    private String oldIndexName;

    /** NORMAL / UNIQUE / FULLTEXT / SPATIAL */
    private String indexType = "NORMAL";

    /** BTREE / HASH */
    private String indexMethod = "BTREE";

    private String comment;

    /** true means UNIQUE (compat) */
    private Boolean uniqueIndex = false;

    private List<String> columns;
}