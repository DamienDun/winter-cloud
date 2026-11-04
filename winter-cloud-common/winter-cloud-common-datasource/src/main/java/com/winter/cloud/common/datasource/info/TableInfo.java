package com.winter.cloud.common.datasource.info;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 表信息
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2023/4/20 13:31
 */
@Getter
@Setter
@NoArgsConstructor
public class TableInfo {

    /**
     * 表名称
     */
    private String name;

    /**
     * 表注释
     */
    private String comment;

    /**
     * 字段列表
     */
    private List<ColumnInfo> columns;
}
