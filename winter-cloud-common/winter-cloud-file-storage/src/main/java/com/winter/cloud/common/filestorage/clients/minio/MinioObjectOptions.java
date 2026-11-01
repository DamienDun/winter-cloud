package com.winter.cloud.common.filestorage.clients.minio;

import io.minio.PutObjectOptions;
import io.minio.ServerSideEncryption;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

/**
 * 对象选项
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2022/8/17 13:28
 */
public class MinioObjectOptions implements Serializable {

    private static final long serialVersionUID = 2005524929692176090L;
    private String contentType = null;
    private Map<String, String> headers = null;
    private ServerSideEncryption sse = null;
    private final long objectSize;
    private final long partSize;

    public MinioObjectOptions(long objectSize, long partSize) {
        if (objectSize < 0L) {
            throw new IllegalArgumentException(
                    "objectSize size " + objectSize + " Cannot be less than 0B。");
        }
        if (objectSize > PutObjectOptions.MAX_OBJECT_SIZE) {
            throw new IllegalArgumentException(
                    "object size " + objectSize + " is not supported; maximum allowed 5TiB");
        }
        if (partSize > 0) {
            if (partSize < PutObjectOptions.MIN_MULTIPART_SIZE) {
                throw new IllegalArgumentException(
                        "part size " + partSize + " is not supported; minimum allowed 5MiB");
            }
            if (partSize > PutObjectOptions.MAX_PART_SIZE) {
                throw new IllegalArgumentException(
                        "part size " + partSize + " is not supported; maximum allowed 5GiB");
            }
            if (partSize > objectSize) {
                partSize = objectSize;
            }
            this.partSize = partSize;
        } else {
            double pSize = Math.ceil((double) objectSize / PutObjectOptions.MAX_MULTIPART_COUNT);
            this.partSize = (long) (Math.ceil(pSize / PutObjectOptions.MIN_MULTIPART_SIZE) * PutObjectOptions.MIN_MULTIPART_SIZE);
        }
        if (this.partSize <= 0) {
            throw new IllegalArgumentException(
                    "valid part size must be provided when object size is unknown");
        }
        this.objectSize = objectSize;

    }

    /**
     * 获取对象总大小
     *
     * @return
     */
    public long getObjectSize() {
        return this.objectSize;
    }

    /**
     * 获取每部份大小
     *
     * @return
     */
    public long getPartSize() {
        return this.partSize;
    }

    public void setHeaders(Map<String, String> headers) {
        if (headers != null) {
            this.headers = Collections.unmodifiableMap(headers);
        } else {
            this.headers = null;
        }
    }

    public Map<String, String> headers() {
        return this.headers;
    }

    public void setSse(ServerSideEncryption sse) {
        this.sse = sse;
    }

    public ServerSideEncryption sse() {
        return this.sse;
    }

    public void setContentType(String contentType) throws IllegalArgumentException {
        if (contentType == null || contentType.equals("")) {
            throw new IllegalArgumentException("invalid content type");
        }
        this.contentType = contentType;
    }

    public String contentType() {
        if (this.contentType != null) {
            return this.contentType;
        }
        if (this.headers == null || this.headers.get("Content-Type") == null) {
            return "application/octet-stream";
        }
        return this.headers.get("Content-Type");
    }
}
