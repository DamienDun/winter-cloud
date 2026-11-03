package com.winter.cloud.system.api;

import com.winter.cloud.common.core.constant.SecurityConstants;
import com.winter.cloud.common.core.constant.ServiceNameConstants;
import com.winter.cloud.common.core.domain.R;
import com.winter.cloud.system.api.factory.RemoteConfigFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 系统参数服务
 */
@FeignClient(contextId = "remoteConfigService", value = ServiceNameConstants.SYSTEM_SERVICE,
        fallbackFactory = RemoteConfigFallbackFactory.class)
public interface RemoteConfigService {

    /**
     * 按键名查询参数值（内部调用，未命中 Redis 时由 system 回源 DB）
     *
     * @param configKey 参数键名
     * @param source    请求来源
     * @return 参数值
     */
    @GetMapping("/config/inner/configKey")
    R<String> getConfigKey(@RequestParam("configKey") String configKey,
                           @RequestHeader(SecurityConstants.FROM_SOURCE) String source);
}
