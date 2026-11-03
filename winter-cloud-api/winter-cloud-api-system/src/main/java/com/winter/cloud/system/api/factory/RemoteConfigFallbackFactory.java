package com.winter.cloud.system.api.factory;

import com.winter.cloud.common.core.domain.R;
import com.winter.cloud.system.api.RemoteConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 系统参数服务降级处理
 */
@Component
public class RemoteConfigFallbackFactory implements FallbackFactory<RemoteConfigService> {

    private static final Logger log = LoggerFactory.getLogger(RemoteConfigFallbackFactory.class);

    @Override
    public RemoteConfigService create(Throwable throwable) {
        log.error("系统参数服务调用失败:{}", throwable.getMessage());
        return (configKey, source) -> R.fail("获取系统参数失败:" + throwable.getMessage());
    }
}
