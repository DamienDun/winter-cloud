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

    private static final String PROPERTY_SOURCE = "winterSwaggerDefaults";

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