package com.winter.cloud.file.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 单个文件上传入参（对齐 portal CommonController / FileUploadInput）
 */
@Getter
@Setter
@NoArgsConstructor
@Schema(description = "单个文件上传入参")
public class FileUploadInput implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "上传的文件不能为空")
    @Schema(description = "上传的文件")
    private MultipartFile file;

    @Schema(description = "是否按加密文件处理（非图片时走解密服务），默认 true")
    private boolean encrypt = true;

    @Schema(description = "指定上传目录（对象路径前缀）")
    private String uploadFolderPath;

    @Schema(description = "指定对象文件名（不含扩展名）")
    private String uploadFileName;

    @Schema(description = "业务模块标识")
    private String module;

    @Schema(description = "文件 MD5，空则服务端计算")
    private String fileMd5;

    /**
     * null：跟随系统参数 sys.upload.picWatermark；
     * true/false：显式覆盖。Feign 仅传 file 时建议置 false，避免头像被加水印。
     */
    @Schema(description = "图片是否加水印；不传则读系统参数 sys.upload.picWatermark")
    private Boolean watermark;
}
