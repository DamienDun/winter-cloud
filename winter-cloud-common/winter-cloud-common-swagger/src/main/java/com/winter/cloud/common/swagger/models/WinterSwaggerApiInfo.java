package com.winter.cloud.common.swagger.models;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * API 文档的主要 配置信息
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2022/8/17 15:38
 */
@Setter
@Getter
public class WinterSwaggerApiInfo implements Serializable {

    private static final long serialVersionUID = 4992282835133138163L;

    /**
     * 标题
     */
    private String title;

    /**
     * 说明
     */
    private String description;

    /**
     * 版本
     */
    private String version;

    /**
     * 许可证
     */
    private String license;

    /**
     * 许可证url
     */
    private String licenseUrl;

    /**
     * 作者
     */
    private String authorName;

    /**
     * 作者url
     */
    private String authorUrl;

    /**
     * 作者电子邮件
     */
    private String authorEmail;

    /**
     * 组集合
     */
    private List<WinterSwaggerApiGroupInfo> groups;

    /**
     * 请问请求参数
     */
    private List<WinterSwaggerApiHeaderParameterInfo> headerParameters;

    /**
     * 实例化
     */
    public WinterSwaggerApiInfo() {
        this.setGroups(new ArrayList<>(16));
        this.setHeaderParameters(new ArrayList<>(5));
    }
}
