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

import java.util.Collections;
import java.util.List;

/**
 * 微服务接口文档。
 * <p>
 * 每个服务固定暴露一组与 {@code spring.application.name} 同名的文档，网关只聚合这一组。
 * {@code @EnableWinterSwagger} 上的业务分组只出现在服务自己的文档页里。
 * 普通 {@link org.springdoc.core.customizers.OpenApiCustomiser} 不会作用到分组，请求头要用 {@link GlobalOpenApiCustomizer}。
 * </p>
 *
 * @author winter
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(WinterSwaggerProperties.class)
@ConditionalOnProperty(name = "swagger.enabled", havingValue = "true", matchIfMissing = true)
public class WinterSwaggerAutoConfiguration {

    /**
     * 服务级文档。网关地址是 /{serviceId}/v3/api-docs/{serviceId}。
     * SpringDoc 一旦存在分组，就不再提供不带组名的 /v3/api-docs。
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

    @Bean
    @ConditionalOnMissingBean(OpenAPI.class)
    public OpenAPI winterOpenApi(WinterSwaggerProperties properties, TokenConfig tokenConfig, Environment environment,
                                 @Value("${winter.version:}") String projectVersion,
                                 @Autowired(required = false) WinterSwaggerApiInfo winterSwaggerApiInfo) {
        String schemeName = tokenConfig.getHeader();
        OpenAPI openApi = new OpenAPI()
                .components(new Components().addSecuritySchemes(schemeName, securityScheme(tokenConfig)))
                .addSecurityItem(new SecurityRequirement().addList(schemeName))
                .info(convertInfo(properties.getInfo(), winterSwaggerApiInfo, projectVersion, serviceName(environment)));
        if (StringUtils.isNotEmpty(properties.getGatewayUrl())) {
            openApi.servers(Collections.singletonList(new Server().url(properties.getGatewayUrl())));
        }
        return openApi;
    }

    /**
     * 把 @EnableWinterSwagger 上的 headerParameters 加到每个接口。
     * 这一 Bean 同时作用于服务级文档和业务分组。
     */
    @Bean
    public GlobalOpenApiCustomizer winterHeaderParameterCustomizer(@Autowired(required = false) WinterSwaggerApiInfo winterSwaggerApiInfo) {
        return openApi -> appendHeaders(openApi, winterSwaggerApiInfo);
    }

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
     * dataType 是参数 schema 的 type，不是 Content-Type。
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

    private SecurityScheme securityScheme(TokenConfig tokenConfig) {
        return new SecurityScheme().type(SecurityScheme.Type.APIKEY)
                .name(tokenConfig.getHeader())
                .in(SecurityScheme.In.HEADER)
                .scheme(tokenConfig.getPrefix());
    }

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
        // 配置文件覆盖注解。许可证对象没配时不能写回 null，否则会清掉注解上的许可证
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

    private Contact createContactInfo(WinterSwaggerApiInfo apiInfo) {
        Contact contact = new Contact();
        contact.setName(apiInfo.getAuthorName());
        contact.setUrl(apiInfo.getAuthorUrl());
        contact.setEmail(apiInfo.getAuthorEmail());
        return contact;
    }

    private String serviceName(Environment environment) {
        String serviceName = environment.getProperty("spring.application.name");
        return StringUtils.isEmpty(serviceName) ? "application" : serviceName;
    }
}
