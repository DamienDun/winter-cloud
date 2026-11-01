package com.winter.cloud.gateway.config;

import com.alibaba.nacos.client.naming.event.InstancesChangeEvent;
import com.alibaba.nacos.common.notify.Event;
import com.alibaba.nacos.common.notify.NotifyCenter;
import com.alibaba.nacos.common.notify.listener.Subscriber;
import com.winter.cloud.common.core.utils.StringUtils;
import org.springdoc.core.AbstractSwaggerUiConfigProperties;
import org.springdoc.core.SwaggerUiConfigProperties;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 网关侧 SpringDoc / Swagger UI 聚合。
 * Knife4j 走 knife4j.gateway 配置；这里只给 /swagger-ui 兜底。
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(value = "springdoc.api-docs.enabled", matchIfMissing = true)
public class SpringDocConfig implements InitializingBean {

    @Autowired
    private SwaggerUiConfigProperties swaggerUiConfigProperties;

    @Autowired
    private DiscoveryClient discoveryClient;

    @Override
    public void afterPropertiesSet() {
        SwaggerDocRegister register = new SwaggerDocRegister(swaggerUiConfigProperties, discoveryClient);
        // 启动时先刷一次，不能只等 Nacos 实例变更事件
        register.refresh();
        NotifyCenter.registerSubscriber(register);
    }
}

/**
 * 按服务名聚合 OpenAPI。发现客户端路径是 /{serviceId}/**，文档地址因此带服务前缀。
 */
class SwaggerDocRegister extends Subscriber<InstancesChangeEvent> {

    private static final String[] EXCLUDE_ROUTES = new String[]{
            "winter-gateway", "winter-auth", "winter-monitor"
    };

    private final SwaggerUiConfigProperties swaggerUiConfigProperties;

    private final DiscoveryClient discoveryClient;

    SwaggerDocRegister(SwaggerUiConfigProperties swaggerUiConfigProperties, DiscoveryClient discoveryClient) {
        this.swaggerUiConfigProperties = swaggerUiConfigProperties;
        this.discoveryClient = discoveryClient;
    }

    @Override
    public void onEvent(InstancesChangeEvent event) {
        refresh();
    }

    void refresh() {
        Set<AbstractSwaggerUiConfigProperties.SwaggerUrl> swaggerUrlSet = new LinkedHashSet<>();
        for (String serviceId : discoveryClient.getServices()) {
            if (StringUtils.equalsAnyIgnoreCase(serviceId, EXCLUDE_ROUTES)) {
                continue;
            }
            if (discoveryClient.getInstances(serviceId).isEmpty()) {
                continue;
            }
            AbstractSwaggerUiConfigProperties.SwaggerUrl swaggerUrl = new AbstractSwaggerUiConfigProperties.SwaggerUrl();
            swaggerUrl.setName(serviceId);
            // 业务服务固定用 spring.application.name 作为分组名
            swaggerUrl.setUrl(String.format("/%s/v3/api-docs/%s", serviceId, serviceId));
            swaggerUrlSet.add(swaggerUrl);
        }
        swaggerUiConfigProperties.setUrls(swaggerUrlSet);
    }

    @Override
    public Class<? extends Event> subscribeType() {
        return InstancesChangeEvent.class;
    }
}
