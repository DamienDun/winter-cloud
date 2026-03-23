package com.winter.cloud.common.swagger.models;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * Api 头参数信息
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2022/8/17 15:38
 */
@Setter
@Getter
public class WinterSwaggerApiHeaderParameterInfo implements Serializable {
    private static final long serialVersionUID = 8587729529013059448L;

    /**
     * 参数名称
     */
    private String name;

    /**
     * 数据类型
     */
    private String dataType;

    /**
     * 说明
     */
    private String description;

    /**
     * 是否必须
     */
    private boolean required;
}
