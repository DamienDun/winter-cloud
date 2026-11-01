package com.winter.cloud.common.filestorage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 上传输出
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2023/12/13 9:50
 */
@Getter
@Setter
@NoArgsConstructor
public class UploadOutput extends UploadStatus {

    private static final long serialVersionUID = 4398545560838876388L;

    /**
     * 本次上传字节数
     */
    @Schema(description = "本次上传字节数")
    private long currentBytes;
}
