package com.winter.cloud.common.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.InputStream;

/**
 * 下载文件信息
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2023/12/13 9:17
 */
@Getter
@Setter
public class DownloadFileInput {

    /**
     * 内容类型
     */
    @Schema(description = "内容类型")
    private String contentType;

    /**
     * 文件大小
     */
    @Schema(description = "文件大小")
    private long fileSize;

    /**
     * 文件名称(含扩展名)
     */
    @Schema(description = "文件名称(含扩展名)")
    private String fileName;

    /**
     * 文件流
     */
    @Schema(description = "文件流")
    private InputStream fileStream;
}
