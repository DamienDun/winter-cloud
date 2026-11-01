package com.winter.cloud.common.filestorage.clients.tencent;

import com.winter.cloud.common.filestorage.properties.AbstractStorageKeyClientProperties;
import com.winter.cloud.common.filestorage.properties.WinterStorageProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2022/8/17 14:22
 */
@ToString(callSuper = true)
@Getter
@Setter
public class TencentStorageClientProperties extends AbstractStorageKeyClientProperties {

    private static final long serialVersionUID = 1980189690450360492L;

    /**
     * bean条件属性
     */
    public static final String BEAN_CONDITIONAL_PROPERTY = WinterStorageProperties.PREFIX + ".tencent.enable";

    /**
     * 通道 Bean 名称
     */
    public static final String CHANNEL_BEAN_NAME = CHANNEL_BEAN_PREFIX + "Tencent" + CHANNEL_BEAN_SUFFIX;
}
