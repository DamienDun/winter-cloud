package com.winter.cloud.common.swagger.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;

/**
 * Swagger 配置属性
 *
 * @author winter
 */
@ConfigurationProperties(prefix = "swagger")
public class WinterSwaggerProperties {

    /**
     * 是否开启接口文档。关闭后不注册 OpenAPI，网关也聚合不到该服务。
     */
    private boolean enabled = true;

    /**
     * 网关对外地址，需要带上该服务的路由前缀。
     * 例如 http://127.0.0.1:8080/winter-system。
     * 留空时按当前请求地址生成，适合直连服务调试。
     */
    private String gatewayUrl;

    /**
     * 文档基本信息
     */
    @NestedConfigurationProperty
    private InfoProperties info = new InfoProperties();

    /**
     * <p>
     * 文档的基础属性信息
     * </p>
     *
     * @see io.swagger.v3.oas.models.info.Info
     * <p>
     * 为了 springboot 自动生产配置提示信息，所以这里复制一个类出来
     */
    public static class InfoProperties {
        /**
         * 标题
         */
        private String title = null;

        /**
         * 描述
         */
        private String description = null;

        /**
         * 联系人信息
         */
        @NestedConfigurationProperty
        private Contact contact = null;

        /**
         * 许可证
         */
        @NestedConfigurationProperty
        private License license = null;

        /**
         * 版本
         */
        private String version = null;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Contact getContact() {
            return contact;
        }

        public void setContact(Contact contact) {
            this.contact = contact;
        }

        public License getLicense() {
            return license;
        }

        public void setLicense(License license) {
            this.license = license;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getGatewayUrl() {
        return gatewayUrl;
    }

    public void setGatewayUrl(String gatewayUrl) {
        this.gatewayUrl = gatewayUrl;
    }

    public InfoProperties getInfo() {
        return info;
    }

    public void setInfo(InfoProperties info) {
        this.info = info;
    }
}
