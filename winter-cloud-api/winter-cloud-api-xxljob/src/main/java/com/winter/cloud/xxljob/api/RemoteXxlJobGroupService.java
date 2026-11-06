package com.winter.cloud.xxljob.api;

import com.winter.cloud.common.core.constant.ServiceNameConstants;
import com.winter.cloud.common.core.web.domain.AjaxResult;
import com.winter.cloud.common.core.web.page.TableDataInfo;
import com.winter.cloud.xxljob.api.factory.RemoteXxlJobGroupFallbackFactory;
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
 * XXL-JOB 执行器管理服务
 */
@FeignClient(contextId = "remoteXxlJobGroupService", value = ServiceNameConstants.XXLJOB_SERVICE,
        fallbackFactory = RemoteXxlJobGroupFallbackFactory.class)
public interface RemoteXxlJobGroupService {

    /**
     * 执行器分页列表
     */
    @GetMapping("/jobgroup/list")
    TableDataInfo list(@RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                       @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
                       @RequestParam(value = "appname", required = false) String appname,
                       @RequestParam(value = "title", required = false) String title);

    /**
     * 全部执行器
     */
    @GetMapping("/jobgroup/all")
    AjaxResult all();

    /**
     * 新增执行器
     */
    @PostMapping("/jobgroup")
    AjaxResult add(@RequestBody Map<String, Object> body);

    /**
     * 修改执行器
     */
    @PutMapping("/jobgroup")
    AjaxResult edit(@RequestBody Map<String, Object> body);

    /**
     * 删除执行器
     */
    @DeleteMapping("/jobgroup/{id}")
    AjaxResult remove(@PathVariable("id") int id);
}
