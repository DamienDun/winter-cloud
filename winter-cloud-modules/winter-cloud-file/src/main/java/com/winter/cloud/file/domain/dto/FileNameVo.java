package com.winter.cloud.file.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 打包下载时的单文件项（可指定 zip 内文件名）
 */
@Getter
@Setter
@NoArgsConstructor
@Schema(description = "文件名称入参")
public class FileNameVo implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "url不能为空")
    @Schema(description = "文件访问地址或对象路径", requiredMode = Schema.RequiredMode.REQUIRED)
    private String url;

    @Schema(description = "zip内指定文件名，为空则取url末段")
    private String specifyFileName;
}