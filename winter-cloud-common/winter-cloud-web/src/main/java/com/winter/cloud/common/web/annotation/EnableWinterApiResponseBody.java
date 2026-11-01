package com.winter.cloud.common.web.annotation;

import com.winter.cloud.common.web.configure.ApiResponseBodyHandlerRegistrar;
import com.winter.cloud.common.web.configure.WinterWebAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * 启用响应体按 Api 格式输出
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2022/12/16 14:14
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@EnableConfigurationProperties({ServerProperties.class})
@Import({ApiResponseBodyHandlerRegistrar.class, WinterWebAutoConfiguration.class})
public @interface EnableWinterApiResponseBody {

    /**
     * Api 控制器包集合属性
     */
    String API_CONTROLLER_PACKAGES_ATTRIBUTE_NAME = "apiControllerPackages";

    /**
     * Api 控制器的包集合,未指定时默认拦截所有 url(除自动识别 url外)
     *
     * @return
     */
    @AliasFor("apiControllerPackages")
    String[] value() default {};

    /**
     * Api 控制器的包集合,未指定时默认拦截所有 url(除自动识别 url外)
     *
     * @return
     */
    @AliasFor("value")
    String[] apiControllerPackages() default {};
}
