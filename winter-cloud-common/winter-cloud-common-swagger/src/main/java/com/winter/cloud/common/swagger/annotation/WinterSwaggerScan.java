package com.winter.cloud.common.swagger.annotation;

import java.lang.annotation.*;

/**
 * Swagger 扫描
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2022/8/17 15:38
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(WinterSwaggerScans.class)
public @interface WinterSwaggerScan {

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
