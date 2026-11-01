package com.winter.cloud.common.filestorage.clients.aliyun;

import com.winter.cloud.common.filestorage.AbstractLocationBucket;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 阿里云分区信息
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
public class AliyunBucket extends AbstractLocationBucket {

    /**
     *
     */
    private static final long serialVersionUID = 6403443988253891221L;

    /**
     * @param name
     */
    public AliyunBucket(String name) {
        super(name);
    }

    /**
     *
     */
    private String extranetEndpoint;

    // Internal endpoint. It could be accessed within AliCloud under the same
    // location.
    private String intranetEndpoint;

}
