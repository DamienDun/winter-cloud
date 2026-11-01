package com.winter.cloud.common.filestorage.clients.aws;

import com.winter.cloud.common.filestorage.AbstractBucket;

/**
 * AWS S3 分区
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2026/7/27
 */
public class AwsBucket extends AbstractBucket {

    private static final long serialVersionUID = 1L;

    /**
     * @param name 分区名称
     */
    public AwsBucket(String name) {
        super(name);
    }
}
