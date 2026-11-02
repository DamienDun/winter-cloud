package com.winter.cloud.file.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.List;

/**
 * 多个文件打包下载 zip 入参
 */
@Getter
@Setter
@NoArgsConstructor
@Schema(description = "多个文件打包下载zip入参")
public class FileDownloadZipInput implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotEmpty(message = "urls不能为空")
    @Schema(description = "全路径url或对象路径集合", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> urls;
}