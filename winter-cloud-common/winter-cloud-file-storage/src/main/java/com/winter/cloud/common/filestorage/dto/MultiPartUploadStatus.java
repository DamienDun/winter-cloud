package com.winter.cloud.common.filestorage.dto;

import com.winter.cloud.common.filestorage.clients.minio.MinioMultiPartUploadOptions;
import com.winter.cloud.common.filestorage.clients.minio.PartUploadTag;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 上传状态
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2023/12/13 14:48
 */
@Getter
@Setter
@NoArgsConstructor
public class MultiPartUploadStatus extends UploadStatus {

    private static final long serialVersionUID = 3964193069485505559L;

    /**
     * 数据库主键
     */
    private Long id;

    /**
     * 选项
     */
    private MinioMultiPartUploadOptions options;

    /**
     * 合计部份
     */
    private List<PartUploadTag> totalParts;
}
