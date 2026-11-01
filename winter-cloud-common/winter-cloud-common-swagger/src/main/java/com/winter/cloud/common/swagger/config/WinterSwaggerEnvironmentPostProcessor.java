package com.winter.cloud.common.swagger.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * 关闭 springdoc 缓存，使 OpenAPI 可按请求区分直连与网关文档（路径前缀是否改写）。
 *
 * @author winter
 */
public class WinterSwaggerEnvironmentPostProcessor implements EnvironmentPostProcessor {

    /** PropertySource 名称，避免重复注册。 */
    private static final String PROPERTY_SOURCE = "winterSwaggerDefaults";

    /**
     * 在环境准备阶段写入 springdoc 默认项：禁用 OpenAPI 缓存。
     * 这样同一进程内直连与经网关拉取文档时，可按请求分别决定是否改写路径前缀。
     */
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (environment.getPropertySources().contains(PROPERTY_SOURCE)) {
            return;
        }
        Map<String, Object> defaults = new HashMap<String, Object>(2);
        defaults.put("springdoc.cache.disabled", "true");
        environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE, defaults));
    }
}