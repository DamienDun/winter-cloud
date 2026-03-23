package com.winter.cloud.common.swagger.annotation;

import java.lang.annotation.*;

/**
 * 启用 winter Swagger
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2022/8/17 15:38
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface EnableWinterSwagger {

    /**
     * 字段 title
     */
    String FIELD_TITLE = "title";

    /**
     * 字段 description
     */
    String FIELD_DESCRIPTION = "description";

    /**
     * 字段 version
     */
    String FIELD_VERSION = "version";

    /**
     * 字段 license
     */
    String FIELD_LICENSE = "license";

    /**
     * 字段 licenseUrl
     */
    String FIELD_LICENSE_URL = "licenseUrl";

    /**
     * 字段 authorName
     */
    String FIELD_AUTHOR_NAME = "authorName";

    /**
     * 字段 authorUrl
     */
    String FIELD_AUTHOR_URL = "authorUrl";

    /**
     * 字段 authorEmail
     */
    String FIELD_AUTHOR_EMAIL = "authorEmail";

    /**
     * 字段 groups
     */
    String FIELD_GROUPS = "groups";

    /**
     * 字段 headerParameters
     */
    String FIELD_HEADER_PARAMETERS = "headerParameters";

    /**
     * 标题
     *
     * @return
     */
    String title() default "winter framework api docs";

    /**
     * API文档描述
     *
     * @return API文档描述
     */
    String description() default "winter framework api docs";

    /**
     * API文档版本
     *
     * @return API文档版本
     */
    String version() default "";

    /**
     * API协议
     *
     * @return API协议
     */
    String license() default "";

    /**
     * API协议地址
     *
     * @return API协议地址
     */
    String licenseUrl() default "";

    /**
     * 作者名字
     *
     * @return 作者名字
     */
    String authorName() default "winter";

    /**
     * 作者主页
     *
     * @return 作者主页
     */
    String authorUrl() default "https://www.winter.com";

    /**
     * 作者邮件地址
     *
     * @return 作者邮件地址
     */
    String authorEmail() default "example@winter.com";

    /**
     * API 组
     *
     * @return API 组
     */
    ApiGroup[] groups();

    /**
     * 请求头参数集合
     *
     * @return
     */
    ApiHeaderParameter[] headerParameters() default {};
}
