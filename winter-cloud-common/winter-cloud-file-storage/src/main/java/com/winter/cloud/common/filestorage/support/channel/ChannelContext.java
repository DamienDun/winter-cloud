package com.winter.cloud.common.filestorage.support.channel;

import java.util.Collection;
import java.util.Set;

/** 通道上下文 */
public interface ChannelContext<TChannel extends Channel> {
    void register(TChannel channel);
    TChannel getChannel(String channelId);
    boolean exist(String channelId);
    boolean removeChannel(String channelId);
    void clearAll();
    Set<String> channelIdSet();
    Collection<TChannel> channels();
}