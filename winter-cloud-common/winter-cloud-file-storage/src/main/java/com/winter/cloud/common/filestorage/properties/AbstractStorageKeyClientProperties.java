package com.winter.cloud.common.filestorage.properties;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 具有访问键的客户端属性
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2022/8/15 19:30
 */
@ToString(callSuper = true)
@Getter
@Setter
public class AbstractStorageKeyClientProperties extends AbstractStorageClientProperties {

    private static final long serialVersionUID = -8331440985821576143L;

    /**
     * 访问键
     */
    private String accessKey = "";

    /**
     * 安全键
     */
    private String secretKey = "";
}
