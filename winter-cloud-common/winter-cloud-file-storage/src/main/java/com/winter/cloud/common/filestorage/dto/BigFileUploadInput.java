package com.winter.cloud.common.filestorage.dto;

import com.winter.cloud.common.filestorage.support.validation.NotEmptyFile;
import com.winter.cloud.common.filestorage.support.validation.NotNullOrBlank;
import com.winter.cloud.common.filestorage.support.validation.DefaultDataValidation;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotNull;

/**
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2023/12/13 19:43
 */
@Getter
@Setter
@NoArgsConstructor
@Schema(description = "大文件上传入参")
public class BigFileUploadInput extends DefaultDataValidation {

    private static final long serialVersionUID = -8198467400128538785L;

    @Schema(description = "文件", required = true)
    @NotEmptyFile(message = "文件不能为空")
    private MultipartFile file;

    @Schema(description = "上传id", required = true)
    @NotNullOrBlank(message = "上传id不能为空")
    private String uploadId;

    @Schema(description = "分块编号", required = true)
    @NotNull(message = "分块编号不能为空")
    private Integer partNumber;

    @Schema(description = "重复提交分片编号时是否覆盖")
    private boolean override;
}
