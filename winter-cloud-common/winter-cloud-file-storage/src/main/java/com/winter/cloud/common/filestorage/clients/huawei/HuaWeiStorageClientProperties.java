package com.winter.cloud.common.filestorage.clients.huawei;

import com.winter.cloud.common.filestorage.properties.AbstractStorageKeyClientProperties;
import com.winter.cloud.common.filestorage.properties.WinterStorageProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 华为云存储属性
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2022/8/17 14:20
 */
@ToString
@Getter
@Setter
public class HuaWeiStorageClientProperties extends AbstractStorageKeyClientProperties {

    private static final long serialVersionUID = -8432594584241699201L;

    /**
     * bean条件属性
     */
    public static final String BEAN_CONDITIONAL_PROPERTY = WinterStorageProperties.PREFIX + ".huawei.enable";

    /**
     * 通道 Bean 名称
     */
    public static final String CHANNEL_BEAN_NAME = CHANNEL_BEAN_PREFIX + "HuaWei" + CHANNEL_BEAN_SUFFIX;


}
