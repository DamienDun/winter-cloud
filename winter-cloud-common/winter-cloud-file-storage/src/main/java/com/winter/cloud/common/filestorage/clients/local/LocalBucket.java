package com.winter.cloud.common.filestorage.clients.local;

import com.winter.cloud.common.filestorage.AbstractLocationBucket;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 本地分区
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
public class LocalBucket extends AbstractLocationBucket {

    private static final long serialVersionUID = 4634807388895689305L;

    /**
     * @param name
     */
    public LocalBucket(String name) {
        super(name);
    }
}