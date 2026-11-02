package com.winter.cloud.file.controller;

import com.winter.cloud.common.core.domain.R;
import com.winter.cloud.common.core.utils.AutoMapUtils;
import com.winter.cloud.common.filestorage.dto.BigFileUploadInput;
import com.winter.cloud.common.filestorage.dto.UploadApplyInput;
import com.winter.cloud.common.filestorage.dto.UploadApplyOutput;
import com.winter.cloud.common.filestorage.dto.UploadInput;
import com.winter.cloud.common.filestorage.dto.UploadOptionsDto;
import com.winter.cloud.common.filestorage.dto.UploadOutput;
import com.winter.cloud.common.filestorage.dto.UploadStatus;
import com.winter.cloud.common.filestorage.enums.FsBigFileUploadTaskStatusEnum;
import com.winter.cloud.common.filestorage.service.BigFileUploadService;
import com.winter.cloud.file.service.IResourceFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

/**
 * 大文件分片上传接口。
 * 存储通道由入参 channelId 或配置 winter.storage.client.multipart-channel / default-channel 决定。
 * 网关 StripPrefix=1 后路径为 /bigFile/**，对外为 /file/bigFile/**
 */
@Tag(name = "大文件分片上传")
@RestController
@RequestMapping("/bigFile")
public class BigFileController {

    @Autowired
    private BigFileUploadService bigFileUploadService;

    @Autowired
    private IResourceFileService resourceFileService;

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
        UploadOutput output = bigFileUploadService.multipartUpload(input);
        recordIfComplete(output);
        return R.ok(output);
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
        UploadStatus status = bigFileUploadService.queryMultipartStatus(input);
        recordIfComplete(status);
        return R.ok();
    }

    /**
     * 大文件上传完成后写入资源表
     */
    private void recordIfComplete(UploadStatus status) {
        if (status == null) {
            return;
        }
        boolean completed = status.isComplete()
            || FsBigFileUploadTaskStatusEnum.COMPLETE_UPLOAD.getCode().equals(status.getStatus());
        if (!completed) {
            return;
        }
        resourceFileService.saveUploadRecord(null, status.getAccessPath(), status.getFilePath(),
            status.getChannelId(), status.getBucketName(), status.getFileMd5(), status.getTotalBytes());
    }
}
