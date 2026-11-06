package com.winter.cloud.xxljob.api.factory;

import com.winter.cloud.common.core.web.domain.AjaxResult;
import com.winter.cloud.common.core.web.page.TableDataInfo;
import com.winter.cloud.xxljob.api.RemoteXxlJobGroupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * XXL-JOB 执行器管理降级处理
 */
@Component
public class RemoteXxlJobGroupFallbackFactory implements FallbackFactory<RemoteXxlJobGroupService> {
    private static final Logger log = LoggerFactory.getLogger(RemoteXxlJobGroupFallbackFactory.class);

    @Override
    public RemoteXxlJobGroupService create(Throwable throwable) {
        log.error("XXL-JOB执行器服务调用失败:{}", throwable.getMessage());
        return new RemoteXxlJobGroupService() {
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
            public TableDataInfo list(Integer pageNum, Integer pageSize, String appname, String title) {
                return failPage("查询执行器列表");
            }

            @Override
            public AjaxResult all() {
                return fail("查询全部执行器");
            }

            @Override
            public AjaxResult add(Map<String, Object> body) {
                return fail("新增执行器");
            }

            @Override
            public AjaxResult edit(Map<String, Object> body) {
                return fail("修改执行器");
            }

            @Override
            public AjaxResult remove(int id) {
                return fail("删除执行器");
            }

            @Override
            public AjaxResult allInner(String source) {
                return fail("内部查询全部执行器");
            }

            @Override
            public AjaxResult addInner(Map<String, Object> body, String source) {
                return fail("内部新增执行器");
            }
        };
    }
}
