package com.winter.cloud.common.swagger.annotation;

import java.lang.annotation.*;

/**
 * Swagger 扫描集合
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
public @interface WinterSwaggerScans {

    /**
     * 扫描集合
     *
     * @return
     */
    WinterSwaggerScan[] value();
}
