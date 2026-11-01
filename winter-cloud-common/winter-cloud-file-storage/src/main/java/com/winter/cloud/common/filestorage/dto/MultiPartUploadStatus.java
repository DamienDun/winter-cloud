package com.winter.cloud.common.filestorage.dto;

import com.winter.cloud.common.filestorage.multipart.MultipartPartETag;
import com.winter.cloud.common.filestorage.multipart.MultipartUploadOptions;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 分片上传运行时状态。
 */
@Getter
@Setter
@NoArgsConstructor
public class MultiPartUploadStatus extends UploadStatus {

    private static final long serialVersionUID = 3964193069485505559L;

    /** 数据库主键 */
    private Long id;

    /** 通道 ID */
    private String channelId;

    /** 分片会话选项 */
    private MultipartUploadOptions options;

    /** 已上传分片 */
    private List<MultipartPartETag> totalParts;
}
