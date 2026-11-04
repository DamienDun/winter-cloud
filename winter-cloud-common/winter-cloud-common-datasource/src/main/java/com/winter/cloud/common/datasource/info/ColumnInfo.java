package com.winter.cloud.common.datasource.info;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 字段信息
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2023/4/20 13:23
 */
@Getter
@Setter
@NoArgsConstructor
public class ColumnInfo {

    /**
     * 字段名称
     */
    private String name;

    /**
     * 字段类型
     */
    private String type;

    /**
     * 字段注释
     */
    private String comment;

    /**
     * 是否主键
     */
    private boolean primarykey;

    /**
     * 是否允许为空,0-不可为空,1-可以为空
     */
    private int allowNull;
}
