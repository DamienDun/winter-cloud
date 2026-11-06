package com.winter.cloud.xxljob.api.factory;

import com.winter.cloud.common.core.web.domain.AjaxResult;
import com.winter.cloud.common.core.web.page.TableDataInfo;
import com.winter.cloud.xxljob.api.RemoteXxlJobLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * XXL-JOB 调度日志降级处理
 */
@Component
public class RemoteXxlJobLogFallbackFactory implements FallbackFactory<RemoteXxlJobLogService> {
    private static final Logger log = LoggerFactory.getLogger(RemoteXxlJobLogFallbackFactory.class);

    @Override
    public RemoteXxlJobLogService create(Throwable throwable) {
        log.error("XXL-JOB调度日志服务调用失败:{}", throwable.getMessage());
        return new RemoteXxlJobLogService() {
            private AjaxResult fail(String action) {
                return AjaxResult.error(action + "失败:" + throwable.getMessage());
            }

            private TableDataInfo failPage(String action) {
                TableDataInfo rsp = new TableDataInfo();
                rsp.setCode(500);
                rsp.setMsg(action + "失败:" + throwable.getMessage());
                return rsp;
            }

            @Override
            public TableDataInfo list(Integer pageNum, Integer pageSize, Integer jobGroup, Integer jobId,
                                      Integer logStatus, String filterTime) {
                return failPage("查询调度日志");
            }

            @Override
            public AjaxResult jobs(int jobGroup) {
                return fail("查询任务下拉");
            }

            @Override
            public AjaxResult detail(String executorAddress, Long triggerTime, Long logId, Integer fromLineNum) {
                return fail("查看执行日志");
            }

            @Override
            public AjaxResult clear(int jobGroup, int jobId, int type) {
                return fail("清理调度日志");
            }

            @Override
            public AjaxResult kill(int id) {
                return fail("终止任务");
            }
        };
    }
}
