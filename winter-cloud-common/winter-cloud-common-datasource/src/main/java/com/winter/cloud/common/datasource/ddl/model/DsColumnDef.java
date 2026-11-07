package com.winter.cloud.common.datasource.ddl.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Column definition for create/alter table
 */
@Getter
@Setter
@NoArgsConstructor
public class DsColumnDef {

    /** column name */
    private String name;

    /** e.g. varchar(64), bigint, decimal(10,2) */
    private String type;

    /** nullable */
    private Boolean nullable = true;

    /** primary key */
    private Boolean primaryKey = false;

    /** default value */
    private String defaultValue;

    /** column comment */
    private String comment;

    /** old column name when renaming */
    private String oldName;

    /**
     * MySQL ADD COLUMN ... AFTER xxx（用于与源表字段顺序对齐）
     */
    private String afterColumn;
}