package com.winter.cloud.common.swagger.config;

import com.winter.cloud.common.core.config.TokenConfig;
import com.winter.cloud.common.core.utils.StringUtils;
import com.winter.cloud.common.swagger.config.properties.WinterSwaggerProperties;
import com.winter.cloud.common.swagger.models.WinterSwaggerApiHeaderParameterInfo;
import com.winter.cloud.common.swagger.models.WinterSwaggerApiInfo;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.GroupedOpenApi;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 微服务 OpenAPI 自动配置。
 * <p>
 * 每个服务暴露与 {@code spring.application.name} 同名的文档组，供网关聚合
 * {@code /{serviceId}/v3/api-docs/{serviceId}}。
 * {@code @EnableWinterSwagger} 的业务分组仅在服务本地文档页可见。
 * 请求头等需用 {@link GlobalOpenApiCustomizer}（普通 OpenApiCustomiser 不作用于分组）。
 * </p>
 *
 * @author winter
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(WinterSwaggerProperties.class)
@ConditionalOnProperty(name = "swagger.enabled", havingValue = "true", matchIfMissing = true)
public class WinterSwaggerAutoConfiguration {

    /**
     * 服务级文档组。SpringDoc 存在分组后不再提供无组名的 /v3/api-docs。
     */
    @Bean
    public GroupedOpenApi winterServiceGroupedOpenApi(Environment environment) {
        String serviceName = serviceName(environment);
        return GroupedOpenApi.builder()
                .group(serviceName)
                .displayName(serviceName)
                .pathsToMatch("/**")
                .build();
    }

    /**
     * 基础 OpenAPI：安全方案与文档信息。servers / 路径前缀由网关自定义器按请求处理。
     */
    @Bean
    @ConditionalOnMissingBean(OpenAPI.class)
    public OpenAPI winterOpenApi(WinterSwaggerProperties properties, TokenConfig tokenConfig, Environment environment,
                                 @Value("${winter.version:}") String projectVersion,
                                 @Autowired(required = false) WinterSwaggerApiInfo winterSwaggerApiInfo) {
        String schemeName = tokenConfig.getHeader();
        return new OpenAPI()
                .components(new Components().addSecuritySchemes(schemeName, securityScheme(tokenConfig)))
                .addSecurityItem(new SecurityRequirement().addList(schemeName))
                .info(convertInfo(properties.getInfo(), winterSwaggerApiInfo, projectVersion, serviceName(environment)));
    }

    /**
     * 为每个 Operation 挂上鉴权方案，并追加 @EnableWinterSwagger 声明的全局请求头。
     * <p>
     * Knife4j 4.x 只认 Operation 上的 {@code security}；仅在 OpenAPI 根节点声明时，
     * Authorize 填了值也不会带到调试请求里。
     * </p>
     */
    @Bean
    public GlobalOpenApiCustomizer winterOperationSecurityCustomizer(
            TokenConfig tokenConfig,
            @Autowired(required = false) WinterSwaggerApiInfo winterSwaggerApiInfo) {
        String schemeName = tokenConfig.getHeader();
        return openApi -> {
            ensureSecurityScheme(openApi, tokenConfig, schemeName);
            applyOperationSecurity(openApi, schemeName);
            appendHeaders(openApi, winterSwaggerApiInfo);
        };
    }

    /**
     * 经网关拉取文档时，把 swagger.gatewayUrl 的路径前缀写入 paths（如 /login → /winter-auth/login），
     * 以兼容 Knife4j 4.x 调试不拼 context-path 的行为；直连服务端口不改写。
     * 需 springdoc.cache.disabled=true（见 WinterSwaggerEnvironmentPostProcessor）。
     */
    @Bean
    public GlobalOpenApiCustomizer winterGatewayPathPrefixCustomizer(WinterSwaggerProperties properties) {
        return openApi -> {
            if (!isRequestViaGateway()) {
                openApi.setServers(null);
                return;
            }
            URI gatewayUri = parseGatewayUri(properties.getGatewayUrl());
            applyGatewayPathPrefix(openApi, pathPrefix(gatewayUri));
            String origin = originOf(gatewayUri, properties.getGatewayUrl());
            if (StringUtils.isNotEmpty(origin)) {
                openApi.setServers(Collections.singletonList(new Server().url(origin)));
            }
        };
    }

    /**
     * 判断当前文档请求是否经网关转发。
     * 网关通常带 X-Forwarded-* / Forwarded；直连服务端口一般没有。
     */
    private boolean isRequestViaGateway() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return false;
        }
        HttpServletRequest request = attributes.getRequest();
        return StringUtils.isNotEmpty(request.getHeader("X-Forwarded-Host"))
                || StringUtils.isNotEmpty(request.getHeader("X-Forwarded-Prefix"))
                || StringUtils.isNotEmpty(request.getHeader("Forwarded"));
    }

    /**
     * 将 OpenAPI paths 统一加上网关路由前缀（已带前缀的 path 不重复追加）。
     */
    private void applyGatewayPathPrefix(OpenAPI openApi, String prefix) {
        if (StringUtils.isEmpty(prefix) || openApi.getPaths() == null || openApi.getPaths().isEmpty()) {
            return;
        }
        Paths rewritten = new Paths();
        for (Map.Entry<String, PathItem> entry : openApi.getPaths().entrySet()) {
            rewritten.addPathItem(withPrefix(prefix, entry.getKey()), entry.getValue());
        }
        openApi.setPaths(rewritten);
    }

    /**
     * 给 path 拼接前缀；path 已等于或已以 prefix/ 开头时原样返回。
     */
    private String withPrefix(String prefix, String path) {
        if (path.equals(prefix) || path.startsWith(prefix + "/")) {
            return path;
        }
        return path.startsWith("/") ? prefix + path : prefix + "/" + path;
    }

    /**
     * 解析 swagger.gatewayUrl；为空或非法时返回 null。
     */
    private URI parseGatewayUri(String gatewayUrl) {
        if (StringUtils.isEmpty(gatewayUrl)) {
            return null;
        }
        try {
            return URI.create(gatewayUrl);
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * 取网关 origin（不含 path）。
     * 例：http://localhost:8080/winter-auth → http://localhost:8080
     */
    private String originOf(URI gatewayUri, String fallback) {
        if (gatewayUri == null || StringUtils.isEmpty(gatewayUri.getScheme()) || StringUtils.isEmpty(gatewayUri.getHost())) {
            return fallback;
        }
        StringBuilder origin = new StringBuilder()
                .append(gatewayUri.getScheme()).append("://").append(gatewayUri.getHost());
        if (gatewayUri.getPort() > 0) {
            origin.append(':').append(gatewayUri.getPort());
        }
        return origin.toString();
    }

    /**
     * 取网关 URL 中的服务路径前缀（去掉末尾 /）。
     * 例：http://localhost:8080/winter-auth → /winter-auth
     */
    private String pathPrefix(URI gatewayUri) {
        if (gatewayUri == null) {
            return null;
        }
        String path = gatewayUri.getPath();
        if (StringUtils.isEmpty(path) || "/".equals(path)) {
            return null;
        }
        if (path.endsWith("/") && path.length() > 1) {
            path = path.substring(0, path.length() - 1);
        }
        return path.startsWith("/") ? path : "/" + path;
    }

    /**
     * 确保 OpenAPI 根上存在指定名称的 SecurityScheme 与全局 SecurityRequirement。
     */
    private void ensureSecurityScheme(OpenAPI openApi, TokenConfig tokenConfig, String schemeName) {
        if (openApi.getComponents() == null) {
            openApi.setComponents(new Components());
        }
        if (openApi.getComponents().getSecuritySchemes() == null
                || !openApi.getComponents().getSecuritySchemes().containsKey(schemeName)) {
            openApi.getComponents().addSecuritySchemes(schemeName, securityScheme(tokenConfig));
        }
        if (openApi.getSecurity() == null || openApi.getSecurity().isEmpty()) {
            openApi.addSecurityItem(new SecurityRequirement().addList(schemeName));
        }
    }

    /**
     * 给尚未声明 security 的 Operation 挂上鉴权方案，供 Knife4j Authorize 生效。
     * 已显式声明（含空数组表示匿名）的接口不覆盖。
     */
    private void applyOperationSecurity(OpenAPI openApi, String schemeName) {
        if (openApi.getPaths() == null) {
            return;
        }
        SecurityRequirement requirement = new SecurityRequirement().addList(schemeName);
        for (PathItem pathItem : openApi.getPaths().values()) {
            for (Operation operation : pathItem.readOperations()) {
                if (operation.getSecurity() != null) {
                    continue;
                }
                operation.addSecurityItem(requirement);
            }
        }
    }

    /**
     * 将 @EnableWinterSwagger.headerParameters 追加到每个 Operation（同名 header 不重复添加）。
     */
    private void appendHeaders(OpenAPI openApi, WinterSwaggerApiInfo apiInfo) {
        if (openApi.getPaths() == null || apiInfo == null || apiInfo.getHeaderParameters() == null) {
            return;
        }
        for (PathItem pathItem : openApi.getPaths().values()) {
            for (Operation operation : pathItem.readOperations()) {
                for (WinterSwaggerApiHeaderParameterInfo header : apiInfo.getHeaderParameters()) {
                    if (StringUtils.isEmpty(header.getName()) || hasHeader(operation, header.getName())) {
                        continue;
                    }
                    operation.addParametersItem(headerParameter(header));
                }
            }
        }
    }

    /**
     * 判断 Operation 是否已包含指定名称的 header 参数。
     */
    private boolean hasHeader(Operation operation, String name) {
        List<Parameter> parameters = operation.getParameters();
        if (parameters == null) {
            return false;
        }
        for (Parameter parameter : parameters) {
            if (name.equals(parameter.getName()) && "header".equals(parameter.getIn())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 将注解中的头参数转为 OpenAPI Parameter。
     * dataType 对应 schema.type，不是 Content-Type。
     */
    private Parameter headerParameter(WinterSwaggerApiHeaderParameterInfo header) {
        String type = StringUtils.isEmpty(header.getDataType()) ? "string" : header.getDataType();
        Schema<String> schema = new Schema<String>();
        schema.setType(type);
        return new Parameter()
                .name(header.getName())
                .description(header.getDescription())
                .required(header.isRequired())
                .in("header")
                .schema(schema);
    }

    /**
     * 构建 HTTP Bearer 安全方案。
     * Authorize 填 token 后自动带 {@code Authorization: Bearer xxx}；网关/服务端会裁剪前缀。
     */
    private SecurityScheme securityScheme(TokenConfig tokenConfig) {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .name(tokenConfig.getHeader())
                .in(SecurityScheme.In.HEADER)
                .description("在 Authorize 中填写登录后的 access_token（无需手动加 Bearer 前缀）");
    }

    /**
     * 合并注解与配置文件中的文档信息；配置优先，缺省时用服务名作 title。
     */
    private Info convertInfo(WinterSwaggerProperties.InfoProperties infoProperties, WinterSwaggerApiInfo apiInfo,
                             String projectVersion, String serviceName) {
        Info info = new Info();
        if (apiInfo != null) {
            info.setTitle(apiInfo.getTitle());
            info.setDescription(apiInfo.getDescription());
            info.setContact(createContactInfo(apiInfo));
            info.setVersion(apiInfo.getVersion());
            if (StringUtils.isNotEmpty(apiInfo.getLicense())) {
                License annotationLicense = new License().name(apiInfo.getLicense());
                if (StringUtils.isNotEmpty(apiInfo.getLicenseUrl())) {
                    annotationLicense.setUrl(apiInfo.getLicenseUrl());
                }
                info.setLicense(annotationLicense);
            }
        }
        if (StringUtils.isEmpty(info.getVersion()) && StringUtils.isNotEmpty(projectVersion)) {
            info.setVersion(projectVersion);
        }
        // 配置覆盖注解；license 未配置时不能 set(null)，否则会清掉注解上的许可证
        if (infoProperties.getLicense() != null) {
            info.setLicense(infoProperties.getLicense());
        }
        if (StringUtils.isNotEmpty(infoProperties.getTitle())) {
            info.setTitle(infoProperties.getTitle());
        }
        if (StringUtils.isNotEmpty(infoProperties.getDescription())) {
            info.setDescription(infoProperties.getDescription());
        }
        if (infoProperties.getContact() != null) {
            info.setContact(infoProperties.getContact());
        }
        if (StringUtils.isNotEmpty(infoProperties.getVersion())) {
            info.setVersion(infoProperties.getVersion());
        }
        if (StringUtils.isEmpty(info.getTitle())) {
            info.setTitle(serviceName);
        }
        return info;
    }

    /**
     * 从注解信息组装 Contact。
     */
    private Contact createContactInfo(WinterSwaggerApiInfo apiInfo) {
        Contact contact = new Contact();
        contact.setName(apiInfo.getAuthorName());
        contact.setUrl(apiInfo.getAuthorUrl());
        contact.setEmail(apiInfo.getAuthorEmail());
        return contact;
    }

    /**
     * 读取 spring.application.name，缺省返回 application。
     */
    private String serviceName(Environment environment) {
        String serviceName = environment.getProperty("spring.application.name");
        return StringUtils.isEmpty(serviceName) ? "application" : serviceName;
    }
}
