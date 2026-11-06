package com.winter.cloud.xxljob.api.factory;

import com.winter.cloud.common.core.web.domain.AjaxResult;
import com.winter.cloud.xxljob.api.RemoteXxlJobDashboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * XXL-JOB 运行报表降级处理
 */
@Component
public class RemoteXxlJobDashboardFallbackFactory implements FallbackFactory<RemoteXxlJobDashboardService> {
    private static final Logger log = LoggerFactory.getLogger(RemoteXxlJobDashboardFallbackFactory.class);

    @Override
    public RemoteXxlJobDashboardService create(Throwable throwable) {
        log.error("XXL-JOB运行报表服务调用失败:{}", throwable.getMessage());
        return new RemoteXxlJobDashboardService() {
            private AjaxResult fail(String action) {
                return AjaxResult.error(action + "失败:" + throwable.getMessage());
            }

            @Override
            public AjaxResult info() {
                return fail("查询报表统计");
            }

            @Override
            public AjaxResult chart(String startDate, String endDate) {
                return fail("查询调度趋势");
            }
        };
    }
}
