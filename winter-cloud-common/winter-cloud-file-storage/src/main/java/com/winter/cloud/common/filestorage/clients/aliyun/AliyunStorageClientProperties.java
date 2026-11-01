package com.winter.cloud.common.filestorage.clients.aliyun;

import com.aliyun.oss.model.CannedAccessControlList;
import com.winter.cloud.common.core.utils.StringUtils;
import com.winter.cloud.common.filestorage.properties.AbstractStorageKeyClientProperties;
import com.winter.cloud.common.filestorage.properties.WinterStorageProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 阿里云存储属性
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2022/8/17 14:14
 */
@ToString(callSuper = true)
@Getter
@Setter
public class AliyunStorageClientProperties extends AbstractStorageKeyClientProperties {

    private static final long serialVersionUID = -8875686980640182432L;

    /**
     * bean条件属性
     */
    public static final String BEAN_CONDITIONAL_PROPERTY = WinterStorageProperties.PREFIX + ".aliyun.enable";

    /**
     * 通道 Bean 名称
     */
    public static final String CHANNEL_BEAN_NAME = CHANNEL_BEAN_PREFIX + "Aliyun" + CHANNEL_BEAN_SUFFIX;

    /**
     * acl权限
     * <p>
     * default、private、public-read、public-read-write
     * </p>
     */
    private String cannedACL = "public-read"; // default、private、public-read、public-read-write

    /**
     * @return
     */
    public CannedAccessControlList toCannedAccessControlList() {
        if (StringUtils.isEmpty(this.getCannedACL())) {
            return CannedAccessControlList.PublicRead;
        }
        try {
            return CannedAccessControlList.parse(this.getCannedACL());
        } catch (Exception e) {
            return CannedAccessControlList.PublicRead;
        }
    }
}
