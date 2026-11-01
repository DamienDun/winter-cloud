package com.winter.cloud.common.filestorage.clients.local;

import com.winter.cloud.common.core.exception.ConfigureException;
import com.winter.cloud.common.core.utils.StringUtils;
import com.winter.cloud.common.filestorage.properties.AbstractStorageClientProperties;
import com.winter.cloud.common.filestorage.properties.WinterStorageProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 本地存储属性
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2022/8/17 14:50
 */
@ToString(callSuper = true)
@Getter
@Setter
public class LocalStorageClientProperties extends AbstractStorageClientProperties {

    private static final long serialVersionUID = 3662026443095670263L;

    /**
     * bean条件属性
     */
    public static final String BEAN_CONDITIONAL_PROPERTY = WinterStorageProperties.PREFIX + ".local.enable";

    /**
     * 通道 Bean 名称
     */
    public static final String CHANNEL_BEAN_NAME = CHANNEL_BEAN_PREFIX + "Local" + CHANNEL_BEAN_SUFFIX;

    /**
     * 默认终节点
     */
    public static final String DEFAULT_ENDPOINT = "/";

    /**
     * 默认分区
     */
    public static final String DEFAULT_BUCKET_NAME = "files";

    /**
     * 根文件路径
     */
    private String rootFilePath = "";

    public LocalStorageClientProperties() {
        this.setEndpoint(DEFAULT_ENDPOINT);
        this.setRootFilePath("");
        this.setDefaultBucketName(DEFAULT_BUCKET_NAME);
    }

    /**
     * 初始化属性
     */
    public void initByProperties() {
        if (StringUtils.isEmpty(this.getEndpoint())) {
            this.setEndpoint(LocalStorageClientProperties.DEFAULT_ENDPOINT);
        }
        if (StringUtils.isEmpty(this.getDefaultBucketName())) {
            this.setDefaultBucketName(LocalStorageClientProperties.DEFAULT_BUCKET_NAME);
        }
        if (StringUtils.isEmpty(this.getRootFilePath())) {
            this.setRootFilePath(LocalStorageClient.getDefaultLocalPath());
        }
        if (StringUtils.isEmpty(this.getRootFilePath())) {
            throw new ConfigureException("本地文件存储无默认保存路径 rootFilePath。");
        }
    }
}
