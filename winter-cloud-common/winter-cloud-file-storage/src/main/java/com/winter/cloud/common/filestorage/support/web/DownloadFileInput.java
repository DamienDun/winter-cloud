package com.winter.cloud.common.filestorage.support.web;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.InputStream;

/** 下载文件入参 */
@Getter
@Setter
public class DownloadFileInput {

    @Schema(description = "内容类型")
    private String contentType;

    @Schema(description = "文件大小")
    private long fileSize;

    @Schema(description = "文件名称(含扩展名)")
    private String fileName;

    @Schema(description = "文件流")
    private InputStream fileStream;
}