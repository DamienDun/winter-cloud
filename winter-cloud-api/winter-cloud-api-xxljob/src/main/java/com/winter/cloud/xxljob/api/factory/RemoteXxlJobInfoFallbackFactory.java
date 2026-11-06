package com.winter.cloud.xxljob.api.factory;

import com.winter.cloud.common.core.web.domain.AjaxResult;
import com.winter.cloud.common.core.web.page.TableDataInfo;
import com.winter.cloud.xxljob.api.RemoteXxlJobInfoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * XXL-JOB 任务管理降级处理
 */
@Component
public class RemoteXxlJobInfoFallbackFactory implements FallbackFactory<RemoteXxlJobInfoService> {
    private static final Logger log = LoggerFactory.getLogger(RemoteXxlJobInfoFallbackFactory.class);

    @Override
    public RemoteXxlJobInfoService create(Throwable throwable) {
        log.error("XXL-JOB任务服务调用失败:{}", throwable.getMessage());
        return new RemoteXxlJobInfoService() {
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
            public TableDataInfo list(Integer pageNum, Integer pageSize, Integer jobGroup, Integer triggerStatus,
                                      String jobDesc, String executorHandler, String author) {
                return failPage("查询任务列表");
            }

            @Override
            public AjaxResult enums() {
                return fail("获取任务枚举");
            }

            @Override
            public AjaxResult add(Map<String, Object> body) {
                return fail("新增任务");
            }

            @Override
            public AjaxResult edit(Map<String, Object> body) {
                return fail("修改任务");
            }

            @Override
            public AjaxResult remove(int id) {
                return fail("删除任务");
            }

            @Override
            public AjaxResult start(int id) {
                return fail("启动任务");
            }

            @Override
            public AjaxResult stop(int id) {
                return fail("停止任务");
            }

            @Override
            public AjaxResult trigger(Map<String, Object> body) {
                return fail("执行任务");
            }

            @Override
            public AjaxResult nextTriggerTime(String scheduleType, String scheduleConf) {
                return fail("计算下次执行时间");
            }

            @Override
            public AjaxResult detail(int id) {
                return fail("查询任务详情");
            }

            @Override
            public AjaxResult saveGlue(Map<String, Object> body) {
                return fail("保存GLUE");
            }

            @Override
            public AjaxResult addInner(Map<String, Object> body, String source) {
                return fail("内部新增任务");
            }

            @Override
            public AjaxResult editInner(Map<String, Object> body, String source) {
                return fail("内部修改任务");
            }

            @Override
            public AjaxResult removeInner(int id, String source) {
                return fail("内部删除任务");
            }

            @Override
            public AjaxResult startInner(int id, String source) {
                return fail("内部启动任务");
            }

            @Override
            public AjaxResult stopInner(int id, String source) {
                return fail("内部停止任务");
            }

            @Override
            public AjaxResult triggerInner(Map<String, Object> body, String source) {
                return fail("内部执行任务");
            }

            @Override
            public AjaxResult nextTriggerTimeInner(String scheduleType, String scheduleConf, String source) {
                return fail("内部计算下次执行时间");
            }
        };
    }
}
