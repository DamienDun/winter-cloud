package com.winter.cloud.file.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.List;

/**
 * 指定名称的多文件打包下载 zip 入参
 */
@Getter
@Setter
@NoArgsConstructor
@Schema(description = "指定名称,多个文件打包下载zip入参")
public class FileDownloadZipSpecifyNameInput implements Serializable {

    private static final long serialVersionUID = 1L;

    @Valid
    @NotEmpty(message = "urls不能为空")
    @Schema(description = "文件列表（含可选指定名称）", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<FileNameVo> urls;
}