package com.winter.cloud.xxljob.api;

import com.winter.cloud.common.core.constant.ServiceNameConstants;
import com.winter.cloud.common.core.web.domain.AjaxResult;
import com.winter.cloud.common.core.web.page.TableDataInfo;
import com.winter.cloud.xxljob.api.factory.RemoteXxlJobInfoFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * XXL-JOB 任务管理服务
 */
@FeignClient(contextId = "remoteXxlJobInfoService", value = ServiceNameConstants.XXLJOB_SERVICE,
        fallbackFactory = RemoteXxlJobInfoFallbackFactory.class)
public interface RemoteXxlJobInfoService {

    /**
     * 任务分页列表
     */
    @GetMapping("/jobinfo/list")
    TableDataInfo list(@RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                       @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
                       @RequestParam(value = "jobGroup", required = false) Integer jobGroup,
                       @RequestParam(value = "triggerStatus", required = false) Integer triggerStatus,
                       @RequestParam(value = "jobDesc", required = false) String jobDesc,
                       @RequestParam(value = "executorHandler", required = false) String executorHandler,
                       @RequestParam(value = "author", required = false) String author);

    /**
     * 任务表单枚举项
     */
    @GetMapping("/jobinfo/enums")
    AjaxResult enums();

    /**
     * 新增任务
     */
    @PostMapping("/jobinfo")
    AjaxResult add(@RequestBody Map<String, Object> body);

    /**
     * 修改任务
     */
    @PutMapping("/jobinfo")
    AjaxResult edit(@RequestBody Map<String, Object> body);

    /**
     * 删除任务
     */
    @DeleteMapping("/jobinfo/{id}")
    AjaxResult remove(@PathVariable("id") int id);

    /**
     * 启动任务
     */
    @PutMapping("/jobinfo/start/{id}")
    AjaxResult start(@PathVariable("id") int id);

    /**
     * 停止任务
     */
    @PutMapping("/jobinfo/stop/{id}")
    AjaxResult stop(@PathVariable("id") int id);

    /**
     * 执行一次
     */
    @PostMapping("/jobinfo/trigger")
    AjaxResult trigger(@RequestBody Map<String, Object> body);

    /**
     * 下次执行时间
     */
    @GetMapping("/jobinfo/nextTriggerTime")
    AjaxResult nextTriggerTime(@RequestParam("scheduleType") String scheduleType,
                               @RequestParam("scheduleConf") String scheduleConf);

    /**
     * 任务详情（含 GLUE 历史）
     */
    @GetMapping("/jobinfo/{id}")
    AjaxResult detail(@PathVariable("id") int id);

    /**
     * 保存 GLUE 源码
     */
    @PostMapping("/jobinfo/glue")
    AjaxResult saveGlue(@RequestBody Map<String, Object> body);
}
