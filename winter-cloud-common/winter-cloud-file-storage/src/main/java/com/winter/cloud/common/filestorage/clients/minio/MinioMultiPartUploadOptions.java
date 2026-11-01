package com.winter.cloud.common.filestorage.clients.minio;

import java.io.Serializable;
import java.util.Map;

/**
 * 多部份上传选项
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2022/8/17 13:26
 */
public class MinioMultiPartUploadOptions implements Serializable {

    private static final long serialVersionUID = 8541333346726751295L;

    private final String bucketName;
    private final String objectName;
    private final String uploadId;
    private final long objectSize;
    private final long partSize;
    private final int partCount;
    private final Map<String, String> ssecHeaders;

    public MinioMultiPartUploadOptions(String bucketName, String objectName, String uploadId, long objectSize, long partSize, Map<String, String> ssecHeaders) {
        this.bucketName = bucketName;
        this.objectName = objectName;
        this.uploadId = uploadId;
        this.objectSize = objectSize;
        this.partSize = partSize;
        this.ssecHeaders = ssecHeaders;
        this.partCount = (int) Math.ceil((double) objectSize / partSize);
    }

    /**
     * 获取桶
     *
     * @return
     */
    public String getBucketName() {
        return this.bucketName;
    }

    /**
     * 获取对象名称
     *
     * @return
     */
    public String getObjectName() {
        return this.objectName;
    }

    /**
     * 获取上传id
     *
     * @return
     */
    public String getUploadId() {
        return this.uploadId;
    }

    /**
     * 获取对象大小
     *
     * @return
     */
    public long getObjectSize() {
        return this.objectSize;
    }

    /**
     * 获取部份大小
     *
     * @return
     */
    public long getPartSize() {
        return this.partSize;
    }

    /**
     * 获取部份总数
     *
     * @return
     */
    public int getPartCount() {
        return this.partCount;
    }

    /**
     * 获取Sse请求头
     *
     * @return
     */
    public Map<String, String> getSsecHeaders() {
        return this.ssecHeaders;
    }
}
