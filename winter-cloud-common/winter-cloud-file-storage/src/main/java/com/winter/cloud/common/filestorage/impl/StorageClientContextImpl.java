package com.winter.cloud.common.filestorage.impl;

import com.winter.cloud.common.filestorage.support.channel.AbstractChannelContext;
import com.winter.cloud.common.filestorage.StorageClient;
import com.winter.cloud.common.filestorage.StorageClientContext;

/**
 * 存储客户实现
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2022/8/17 13:18
 */
public class StorageClientContextImpl extends AbstractChannelContext<StorageClient> implements StorageClientContext {

    /**
     *
     */
    public StorageClientContextImpl() {
        super(16);
    }
}
