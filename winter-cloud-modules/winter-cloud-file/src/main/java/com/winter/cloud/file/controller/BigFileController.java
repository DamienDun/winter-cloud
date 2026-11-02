package com.winter.cloud.file.controller;

import com.winter.cloud.common.core.constant.Constants;
import com.winter.cloud.common.core.domain.R;
import com.winter.cloud.common.core.utils.AutoMapUtils;
import com.winter.cloud.common.core.web.controller.BaseController;
import com.winter.cloud.common.core.web.domain.AjaxResult;
import com.winter.cloud.common.core.web.page.TableDataInfo;
import com.winter.cloud.common.filestorage.domain.FsBigFileUploadTask;
import com.winter.cloud.common.filestorage.dto.BigFileUploadInput;
import com.winter.cloud.common.filestorage.dto.UploadApplyInput;
import com.winter.cloud.common.filestorage.dto.UploadApplyOutput;
import com.winter.cloud.common.filestorage.dto.UploadInput;
import com.winter.cloud.common.filestorage.dto.UploadOptionsDto;
import com.winter.cloud.common.filestorage.dto.UploadOutput;
import com.winter.cloud.common.filestorage.dto.UploadStatus;
import com.winter.cloud.common.filestorage.service.BigFileUploadService;
import com.winter.cloud.common.filestorage.service.FsBigFileUploadTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;

/**
 * 大文件分片上传接口。
 * 任务状态落 fs_big_file_upload_task，不再写入 ResourceFile。
 * 存储通道由入参 channelId 或配置 winter.storage.client.multipart-channel / default-channel 决定。
 * 网关 StripPrefix=1 后路径为 /bigFile/**，对外为 /file/bigFile/**
 */
@Tag(name = "大文件分片上传")
@RestController
@RequestMapping("/bigFile")
public class BigFileController extends BaseController {

    @Autowired
    private BigFileUploadService bigFileUploadService;

    @Autowired
    private FsBigFileUploadTaskService fsBigFileUploadTaskService;

    /**
     * 大文件任务分页列表
     */
    @Operation(summary = "大文件任务分页查询")
    @GetMapping("/list")
    public TableDataInfo list(FsBigFileUploadTask query) {
        startPage();
        List<FsBigFileUploadTask> list = fsBigFileUploadTaskService.selectTaskList(query);
        return getDataTable(list);
    }

    /**
     * 大文件任务详情
     */
    @Operation(summary = "大文件任务详情")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable("id") String id) {
        return success(fsBigFileUploadTaskService.selectById(Long.valueOf(id)));
    }

    /**
     * 按主键删除大文件任务（未完成中止分片；已完成删对象）
     */
    @Operation(summary = "按主键删除大文件任务")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable String ids) {
        Long[] idArr = Arrays.stream(ids.split(Constants.ENGLISH_COMMA))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(Long::valueOf)
            .toArray(Long[]::new);
        return toAjax(fsBigFileUploadTaskService.deleteByIds(idArr));
    }

    @Operation(summary = "上传申请")
    @PostMapping("/upload/apply")
    public R<UploadApplyOutput> apply(@RequestBody @Valid UploadApplyInput input) {
        return R.ok(bigFileUploadService.multipartUploadApply(input));
    }

    @Operation(summary = "上传状态")
    @PostMapping("/upload/status")
    public R<UploadStatus> queryStatus(@RequestBody @Valid UploadOptionsDto input) {
        return R.ok(bigFileUploadService.queryMultipartStatus(input));
    }

    @Operation(summary = "上传分片")
    @PostMapping("/upload")
    public R<UploadOutput> upload(@Valid BigFileUploadInput bigFileUploadInput) throws Exception {
        UploadInput input = AutoMapUtils.map(bigFileUploadInput, UploadInput.class);
        input.setInputStream(bigFileUploadInput.getFile().getInputStream());
        return R.ok(bigFileUploadService.multipartUpload(input));
    }

    @Operation(summary = "取消上传")
    @PostMapping("/upload/cancel")
    public R<Void> cancelUpload(@RequestBody @Valid UploadOptionsDto input) {
        bigFileUploadService.deleteMultipartUpload(input);
        return R.ok();
    }

    @Operation(summary = "下载")
    @GetMapping(value = "/download", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void download(@RequestParam(value = "channelId", required = false) String channelId,
                         @RequestParam("bucketName") String bucketName,
                         @RequestParam("path") String path,
                         HttpServletRequest request,
                         HttpServletResponse response) throws Exception {
        // channelId 为空时走配置的 multipart-channel / default-channel
        bigFileUploadService.download(channelId, bucketName, path, request, response);
    }

    @Operation(summary = "手动合并")
    @PostMapping("/merge")
    public R<Void> merge(@RequestBody @Valid UploadOptionsDto input) throws Exception {
        bigFileUploadService.merge(input.getUploadId());
        return R.ok();
    }
}