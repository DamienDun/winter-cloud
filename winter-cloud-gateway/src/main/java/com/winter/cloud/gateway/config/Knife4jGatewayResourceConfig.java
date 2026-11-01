package com.winter.cloud.gateway.config;

import org.springframework.core.Ordered;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Spring Cloud Gateway 的路由层经常先把 /webjars/** 打成 404，
 * 默认 ResourceHandler 来不及处理。这里用最高优先级 WebFilter 直接回源 classpath。
 */
@Component
public class Knife4jGatewayResourceConfig implements WebFilter, Ordered {

    private static final String RESOURCE_ROOT = "META-INF/resources";

    private static final Set<String> EXACT_FILES = new HashSet<String>(Arrays.asList(
            "/doc.html",
            "/favicon.ico"
    ));

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (!isKnife4jStaticPath(path)) {
            return chain.filter(exchange);
        }

        Resource resource = resolveResource(path);
        if (resource == null || !resource.exists() || !resource.isReadable()) {
            exchange.getResponse().setStatusCode(HttpStatus.NOT_FOUND);
            return exchange.getResponse().setComplete();
        }

        MediaType mediaType = MediaTypeFactory.getMediaType(resource)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
        exchange.getResponse().getHeaders().setContentType(mediaType);
        return exchange.getResponse().writeWith(
                Mono.fromCallable(() -> exchange.getResponse().bufferFactory().wrap(readBytes(resource))));
    }

    private boolean isKnife4jStaticPath(String path) {
        return EXACT_FILES.contains(path)
                || path.startsWith("/webjars/")
                || path.startsWith("/img/");
    }

    private Resource resolveResource(String path) {
        if ("/favicon.ico".equals(path)) {
            return new ClassPathResource(RESOURCE_ROOT + "/img/icons/favicon-32x32.png");
        }
        // 防路径穿越
        String normalized = path.replace("\\", "/");
        if (normalized.contains("..")) {
            return null;
        }
        return new ClassPathResource(RESOURCE_ROOT + normalized);
    }

    private byte[] readBytes(Resource resource) throws java.io.IOException {
        java.io.InputStream inputStream = resource.getInputStream();
        try {
            java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }
            return outputStream.toByteArray();
        } finally {
            inputStream.close();
        }
    }

    @Override
    public int getOrder() {
        // 比网关鉴权、路由异常处理更靠前
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
