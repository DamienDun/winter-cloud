package com.winter.cloud.common.filestorage.properties;

import com.winter.cloud.common.core.config.WinterConfig;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * 存储客户端抽象属性
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2022/8/15 19:28
 */
@ToString(callSuper = true)
@Getter
@Setter
public class AbstractStorageClientProperties implements Serializable {

    private static final long serialVersionUID = -225640249927480330L;

    /**
     * 通道 Bean 前缀
     */
    public static final String CHANNEL_BEAN_PREFIX = WinterConfig.PREFIX;

    /**
     * 通道 Bean 后缀
     */
    public static final String CHANNEL_BEAN_SUFFIX = "StorageClient";

    /**
     * 是否启用
     */
    private boolean enable = false;
    /**
     * 终节点
     */
    private String endpoint;
    /**
     * 默认分区
     */
    private String defaultBucketName;
    /**
     * 读数据块大小
     */
    private int readBlockSize = 2048;
    /**
     * 写数据块大小
     */
    private int writeBlockSize = 2048;
}
