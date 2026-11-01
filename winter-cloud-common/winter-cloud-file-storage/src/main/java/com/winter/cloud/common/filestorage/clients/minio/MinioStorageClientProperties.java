package com.winter.cloud.common.filestorage.clients.minio;

import com.winter.cloud.common.filestorage.properties.AbstractStorageKeyClientProperties;
import com.winter.cloud.common.filestorage.properties.WinterStorageProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Minio 存储客户端属性
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2022/8/15 19:27
 */
@ToString
@Getter
@Setter
public class MinioStorageClientProperties extends AbstractStorageKeyClientProperties {

    private static final long serialVersionUID = -6332425646084541191L;

    /**
     * bean条件属性
     */
    public static final String BEAN_CONDITIONAL_PROPERTY = WinterStorageProperties.PREFIX + ".minio.enable";

    /**
     * 通道 Bean 名称
     */
    public static final String CHANNEL_BEAN_NAME = CHANNEL_BEAN_PREFIX + "Minio" + CHANNEL_BEAN_SUFFIX;

    /**
     * Minio 服务器地址
     */
    private String serverUrl = "http://127.0.0.1:9000";
}
