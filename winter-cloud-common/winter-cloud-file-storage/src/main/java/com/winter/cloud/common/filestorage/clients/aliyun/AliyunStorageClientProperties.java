package com.winter.cloud.common.filestorage.clients.aliyun;

import com.winter.cloud.common.filestorage.properties.AbstractStorageKeyClientProperties;
import com.winter.cloud.common.filestorage.properties.WinterStorageProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 阿里云存储属性（不依赖 OSS SDK 类型，便于 SDK optional）
 */
@ToString(callSuper = true)
@Getter
@Setter
public class AliyunStorageClientProperties extends AbstractStorageKeyClientProperties {

    private static final long serialVersionUID = -8875686980640182432L;

    public static final String BEAN_CONDITIONAL_PROPERTY = WinterStorageProperties.PREFIX + ".aliyun.enable";

    public static final String CHANNEL_BEAN_NAME = CHANNEL_BEAN_PREFIX + "Aliyun" + CHANNEL_BEAN_SUFFIX;

    /**
     * ACL：default、private、public-read、public-read-write
     */
    private String cannedACL = "public-read";
}
