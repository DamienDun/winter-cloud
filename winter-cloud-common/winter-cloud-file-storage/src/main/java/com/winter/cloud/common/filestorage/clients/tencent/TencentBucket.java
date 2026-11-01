package com.winter.cloud.common.filestorage.clients.tencent;

import com.winter.cloud.common.filestorage.AbstractLocationBucket;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 腾讯云分区
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2022/8/17 14:20
 */
@ToString(callSuper = true)
@Getter
@Setter
public class TencentBucket extends AbstractLocationBucket {

    private static final long serialVersionUID = 4041836115522694859L;

    /**
     * @param name
     */
    public TencentBucket(String name) {
        super(name);
    }

}
