package com.winter.cloud.common.filestorage.dto;

import com.winter.cloud.common.core.exception.ServiceException;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2023/12/13 9:52
 */
@Getter
@Setter
@NoArgsConstructor
public class UploadInput extends UploadOptionsDto {

    private static final long serialVersionUID = 8996851282584599059L;

    /**
     * 输入流
     */
    @Schema(description = "输入流")
    private InputStream inputStream;

    /**
     * 是否Zip压缩
     */
    @Schema(description = "是否Zip压缩")
    private boolean zipCompress;

    /**
     * 输入字节
     */
    @Schema(description = "输入字节")
    private byte[] bytes;

    @Schema(description = "分片编号")
    private Integer partNumber;

    @Schema(description = "重复提交分片编号时是否覆盖")
    private boolean override;

    @Override
    public void valid() {
        super.valid();
        if (this.getBytes() != null) {
            if (this.getBytes().length == 0) {
                throw new ServiceException("至少二进制字节不能空。");
            }
            this.setInputStream(new ByteArrayInputStream(this.getBytes()));
        } else {
            if (this.getInputStream() == null) {
                throw new ServiceException("输入流不能为空。");
            }
        }
    }
}
