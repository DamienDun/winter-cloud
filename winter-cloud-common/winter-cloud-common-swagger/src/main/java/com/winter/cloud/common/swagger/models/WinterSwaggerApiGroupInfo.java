package com.winter.cloud.common.swagger.models;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * API 文档的分组信息
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2022/8/17 15:38
 */
@Setter
@Getter
public class WinterSwaggerApiGroupInfo implements Serializable {

    private static final long serialVersionUID = 7672594627611848627L;

    /**
     * Bean id
     */
    private String beanId;

    /**
     * 组名称
     */
    private String groupName;

    /**
     * 顺序
     */
    private int order;

    /**
     * 包集合
     */
    private List<String> packages;

    /**
     * 分组注解类型
     */
    private Class<? extends Annotation> annotation;

    /**
     * 实例化
     */
    public WinterSwaggerApiGroupInfo() {
        this.setBeanId("winterSwaggerApiGroup_" + UUID.randomUUID().toString().replace("-", ""));
        this.order = 0;
        this.setPackages(new ArrayList<>(5));
    }

}
