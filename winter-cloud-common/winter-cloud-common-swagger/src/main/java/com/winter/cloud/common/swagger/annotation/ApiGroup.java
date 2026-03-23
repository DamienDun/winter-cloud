package com.winter.cloud.common.swagger.annotation;

import java.lang.annotation.*;

/**
 * Swagger组信息
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
public @interface ApiGroup {

    /**
     * 字段 groupName
     */
    String FIELD_GROUP_NAME = "groupName";

    /**
     * 字段 顺序
     */
    String FIELD_ORDER = "order";

    /**
     * 字段 packages
     */
    String FIELD_PACKAGES = "packages";

    /**
     * 字段 annotation
     */
    String FIELD_ANNOTATION = "annotation";

    /**
     * 组名称
     *
     * @return 组名称
     */
    String groupName() default "default";

    /**
     * 顺序
     *
     * @return
     */
    int order() default 0;

    /**
     * 组包含的 Api 的包集合
     *
     * @return 组包含的 Api 的包集合
     */
    String[] packages() default {};

    /**
     * 组包含的 API 的注解
     *
     * @return 组包含的 API 的注解
     */
    Class<? extends Annotation> annotation() default Annotation.class;
}
