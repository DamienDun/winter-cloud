package com.winter.cloud.common.web.annotation;

import java.lang.annotation.*;

/**
 * 忽略 Api 响应体输出
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2022/12/16 14:09
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Documented
public @interface IgnoreApiResponseBody {
}
