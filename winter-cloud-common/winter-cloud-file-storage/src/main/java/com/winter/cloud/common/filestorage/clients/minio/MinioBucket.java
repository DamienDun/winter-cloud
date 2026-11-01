package com.winter.cloud.common.filestorage.clients.minio;

import com.winter.cloud.common.filestorage.AbstractBucket;

/**
 * Minio 分区
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2022/8/17 13:21
 */
public class MinioBucket extends AbstractBucket {

    private static final long serialVersionUID = 1903403248595667122L;

    /**
     * @param name
     */
    public MinioBucket(String name) {
        super(name);
    }
}
