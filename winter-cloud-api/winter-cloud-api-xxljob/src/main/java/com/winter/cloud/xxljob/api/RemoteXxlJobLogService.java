package com.winter.cloud.xxljob.api;

import com.winter.cloud.common.core.constant.ServiceNameConstants;
import com.winter.cloud.common.core.web.domain.AjaxResult;
import com.winter.cloud.common.core.web.page.TableDataInfo;
import com.winter.cloud.xxljob.api.factory.RemoteXxlJobLogFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * XXL-JOB 调度日志服务
 */
@FeignClient(contextId = "remoteXxlJobLogService", value = ServiceNameConstants.XXLJOB_SERVICE,
        fallbackFactory = RemoteXxlJobLogFallbackFactory.class)
public interface RemoteXxlJobLogService {

    /**
     * 调度日志分页
     */
    @GetMapping("/joblog/list")
    TableDataInfo list(@RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                       @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
                       @RequestParam(value = "jobGroup", required = false) Integer jobGroup,
                       @RequestParam(value = "jobId", required = false) Integer jobId,
                       @RequestParam(value = "logStatus", required = false) Integer logStatus,
                       @RequestParam(value = "filterTime", required = false) String filterTime);

    /**
     * 按执行器查任务下拉
     */
    @GetMapping("/joblog/jobs")
    AjaxResult jobs(@RequestParam("jobGroup") int jobGroup);

    /**
     * 查看执行日志（滚动）
     */
    @GetMapping("/joblog/detail")
    AjaxResult detail(@RequestParam(value = "executorAddress", required = false) String executorAddress,
                      @RequestParam(value = "triggerTime", required = false) Long triggerTime,
                      @RequestParam(value = "logId", required = false) Long logId,
                      @RequestParam(value = "fromLineNum", defaultValue = "1") Integer fromLineNum);

    /**
     * 清理调度日志
     */
    @DeleteMapping("/joblog/clear")
    AjaxResult clear(@RequestParam("jobGroup") int jobGroup,
                     @RequestParam("jobId") int jobId,
                     @RequestParam("type") int type);

    /**
     * 终止任务执行
     */
    @PostMapping("/joblog/kill/{id}")
    AjaxResult kill(@PathVariable("id") int id);
}
