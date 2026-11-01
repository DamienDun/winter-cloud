package com.winter.cloud.common.filestorage.multipart;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

/**
 * 通用分片上传会话选项。
 */
public class MultipartUploadOptions implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String channelId;
    private final String bucketName;
    private final String objectName;
    private final String uploadId;
    private final long objectSize;
    private final long partSize;
    private final int partCount;
    private final String contentType;
    private final Map<String, String> headers;

    public MultipartUploadOptions(String channelId, String bucketName, String objectName, String uploadId,
                                  long objectSize, long partSize, String contentType, Map<String, String> headers) {
        this.channelId = channelId;
        this.bucketName = bucketName;
        this.objectName = objectName;
        this.uploadId = uploadId;
        this.objectSize = objectSize;
        this.partSize = partSize;
        this.partCount = (int) Math.ceil((double) objectSize / partSize);
        this.contentType = contentType;
        this.headers = headers == null ? Collections.emptyMap() : headers;
    }

    public String getChannelId() {
        return channelId;
    }

    public String getBucketName() {
        return bucketName;
    }

    public String getObjectName() {
        return objectName;
    }

    public String getUploadId() {
        return uploadId;
    }

    public long getObjectSize() {
        return objectSize;
    }

    public long getPartSize() {
        return partSize;
    }

    public int getPartCount() {
        return partCount;
    }

    public String getContentType() {
        return contentType;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }
}
