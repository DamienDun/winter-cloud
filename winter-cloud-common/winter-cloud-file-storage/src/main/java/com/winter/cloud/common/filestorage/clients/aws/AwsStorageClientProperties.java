package com.winter.cloud.common.filestorage.clients.aws;

import com.winter.cloud.common.filestorage.properties.AbstractStorageKeyClientProperties;
import com.winter.cloud.common.filestorage.properties.WinterStorageProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * AWS S3 对象存储客户端属性
 * <p>
 * 基于 AWS S3 Java SDK 对接 S3 兼容对象存储
 * </p>
 *
 * @author Damien
 * @description
 * @create 2026/7/27
 */
@ToString
@Getter
@Setter
public class AwsStorageClientProperties extends AbstractStorageKeyClientProperties {

    private static final long serialVersionUID = 1L;

    /**
     * bean条件属性
     */
    public static final String BEAN_CONDITIONAL_PROPERTY = WinterStorageProperties.PREFIX + ".aws.enable";

    /**
     * 通道 Bean 名称
     */
    public static final String CHANNEL_BEAN_NAME = CHANNEL_BEAN_PREFIX + "Aws" + CHANNEL_BEAN_SUFFIX;

    /**
     * S3 服务地址（Endpoint）
     */
    private String serverUrl = "http://127.0.0.1:80";

    /**
     * 区域，S3 兼容存储可填任意合法值，默认 us-east-1
     */
    private String region = "us-east-1";

    /**
     * 是否启用 path-style 访问
     */
    private boolean pathStyleAccess = true;
}
