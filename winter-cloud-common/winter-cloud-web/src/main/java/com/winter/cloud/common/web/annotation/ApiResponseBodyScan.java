package com.winter.cloud.common.web.annotation;

import java.lang.annotation.*;

/**
 * Api响应体扫描
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2022/12/16 14:41
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ApiResponseBodyScan {

    /**
     * 控制器包集合
     *
     * @return
     */
    String[] value();
}
