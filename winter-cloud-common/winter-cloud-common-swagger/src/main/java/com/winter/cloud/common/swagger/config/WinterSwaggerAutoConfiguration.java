package com.winter.cloud.common.swagger.config;

import com.winter.cloud.common.core.config.TokenConfig;
import com.winter.cloud.common.core.utils.StringUtils;
import com.winter.cloud.common.swagger.config.properties.WinterSwaggerProperties;
import com.winter.cloud.common.swagger.models.WinterSwaggerApiInfo;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Swagger 文档配置
 *
 * @author winter
 */
@EnableConfigurationProperties(WinterSwaggerProperties.class)
@ConditionalOnProperty(name = "swagger.enabled", havingValue = "true", matchIfMissing = true)
public class WinterSwaggerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(OpenAPI.class)
    @ConditionalOnBean(WinterSwaggerApiInfo.class)
    public OpenAPI openApi(WinterSwaggerProperties properties, TokenConfig tokenConfig, WinterSwaggerApiInfo winterSwaggerApiInfo) {
        return new OpenAPI()
                .components(new Components()
                        // 设置认证的请求头
                        .addSecuritySchemes(SecurityScheme.Type.APIKEY.toString(), securityScheme(tokenConfig)))
                .addSecurityItem(new SecurityRequirement().addList(SecurityScheme.Type.APIKEY.toString()))
                .info(convertInfo(properties.getInfo(), winterSwaggerApiInfo))
                .servers(servers(properties.getGatewayUrl()));
    }

    @Bean
    @ConditionalOnMissingBean(OpenAPI.class)
    public OpenAPI openApi2(WinterSwaggerProperties properties, TokenConfig tokenConfig) {
        return new OpenAPI()
                .components(new Components()
                        // 设置认证的请求头
                        .addSecuritySchemes(SecurityScheme.Type.APIKEY.toString(), securityScheme(tokenConfig)))
                .addSecurityItem(new SecurityRequirement().addList(SecurityScheme.Type.APIKEY.toString()))
                .info(convertInfo(properties.getInfo(), null))
                .servers(servers(properties.getGatewayUrl()));
    }

    /**
     * 安全模式，这里指定token通过Authorization头请求头传递
     */
    private SecurityScheme securityScheme(TokenConfig tokenConfig) {
        return new SecurityScheme().type(SecurityScheme.Type.APIKEY)
                .name(tokenConfig.getHeader())
                .in(SecurityScheme.In.HEADER)
                .scheme(tokenConfig.getPrefix());
    }

    /**
     * 填充配置信息
     *
     * @param infoProperties
     * @return
     */
    private Info convertInfo(WinterSwaggerProperties.InfoProperties infoProperties, WinterSwaggerApiInfo winterSwaggerApiInfo) {
        Info info = new Info();
        if (Objects.nonNull(winterSwaggerApiInfo)) {
            info.setTitle(winterSwaggerApiInfo.getTitle());
            info.setDescription(winterSwaggerApiInfo.getDescription());
            info.setContact(createContactInfo(winterSwaggerApiInfo));
            info.setVersion(winterSwaggerApiInfo.getVersion());
        }
        //优先使用配置文件,如果没有则使用本地注解
        String title = infoProperties.getTitle();
        String description = infoProperties.getDescription();
        Contact contact = infoProperties.getContact();
        License license = infoProperties.getLicense();
        String version = infoProperties.getVersion();
        info.setLicense(license);
        if (StringUtils.isNotEmpty(title)) {
            info.setTitle(title);
        }
        if (StringUtils.isNotEmpty(description)) {
            info.setDescription(description);
        }
        if (Objects.nonNull(contact)) {
            info.setContact(contact);
        }
        if (StringUtils.isNotEmpty(version)) {
            info.setVersion(version);
        }
        return info;
    }

    /**
     * 创建 API 作者信息
     *
     * @param apiInfo aip信息
     * @return 作者信息
     */
    private Contact createContactInfo(WinterSwaggerApiInfo apiInfo) {
        Contact contact = new Contact();
        contact.setName(apiInfo.getAuthorName());
        contact.setUrl(apiInfo.getAuthorUrl());
        contact.setEmail(apiInfo.getAuthorEmail());
        return contact;
    }

    public List<Server> servers(String gatewayUrl) {
        List<Server> serverList = new ArrayList<>();
        serverList.add(new Server().url(gatewayUrl));
        return serverList;
    }
}
