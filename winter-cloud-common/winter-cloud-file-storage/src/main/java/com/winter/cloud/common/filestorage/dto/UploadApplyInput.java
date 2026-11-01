package com.winter.cloud.common.filestorage.dto;

import com.winter.cloud.common.filestorage.multipart.MultipartUploadConstants;
import com.winter.cloud.common.filestorage.support.validation.DefaultDataValidation;
import com.winter.cloud.common.filestorage.support.validation.NotNullOrBlank;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/**
 * 上传申请输入
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2023/12/13 9:44
 */
@Getter
@Setter
@NoArgsConstructor
@Schema(description = "上传申请输入")
public class UploadApplyInput extends DefaultDataValidation {

    private static final long serialVersionUID = 7126687121797702649L;

    /**
     * 文件总大小
     */
    @Schema(description = "文件总大小", required = true)
    @Min(value = 1L, message = "文件总大小不能小于或等于零。")
    @Max(value = MultipartUploadConstants.MAX_OBJECT_SIZE, message = "文件总大小不能超过5TiB。")
    private long totalBytes;

    /**
     * 文件类型(扩展名)
     */
    @Schema(description = "文件类型(扩展名)", required = true)
    @NotNullOrBlank(message = "文件类型不能为空。")
    private String fileType;

    /**
     * 文件路径(含文件名与类型，如 /a/b/123.txt)
     */
    @Schema(description = "文件路径(含文件名与类型，如 /a/b/123.txt)", required = true)
    @NotNullOrBlank(message = "保存路径不能为空。")
    private String filePath;

    /**
     * 分片字节数
     */
    @Schema(description = "分片字节数(不传系统则默认10M)")
    @Max(value = MultipartUploadConstants.MAX_PART_SIZE, message = "分片大小不能超过5GiB。")
    private Long partBytes;

    /**
     * 文件的Md5值
     */
    @Schema(description = "文件的Md5值")
    private String fileMd5;

    @Schema(description = "要上传的桶,不传则使用默认")
    private String bucketName;

    /**
     * 存储通道：Minio / Aws / aliyunOOS / HuaWeiOBS / TencentCOS；为空则使用配置 multipart-channel
     */
    @Schema(description = "存储通道ID，为空则使用 winter.storage.client.multipart-channel")
    private String channelId;
}
