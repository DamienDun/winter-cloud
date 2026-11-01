package com.winter.cloud.common.filestorage.multipart;

import java.util.List;

/**
 * 大文件分片上传能力（S3 兼容存储均可实现）。
 */
public interface MultipartUploadSupport {

    /**
     * 初始化分片上传，返回会话选项。
     */
    MultipartUploadOptions initiateMultipartUpload(String bucketName, String objectName,
                                                   long objectSize, long partSize, String contentType) throws Exception;

    /**
     * 上传单个分片。
     */
    MultipartPartETag uploadPart(MultipartUploadOptions options, byte[] data, int partNumber, int availableSize) throws Exception;

    /**
     * 列出已上传分片。
     */
    List<MultipartListedPart> listMultipartParts(String bucketName, String objectName, String uploadId) throws Exception;

    /**
     * 合并分片。
     */
    void completeMultipartUpload(String bucketName, String objectName, String uploadId,
                                 List<MultipartPartETag> parts) throws Exception;

    /**
     * 取消分片上传。
     */
    void abortMultipartUpload(String bucketName, String objectName, String uploadId) throws Exception;
}
