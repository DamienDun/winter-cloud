package com.winter.cloud.common.filestorage.multipart;

import java.io.Serializable;

/**
 * 分片 ETag。
 */
public class MultipartPartETag implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int partNumber;
    private final String etag;

    public MultipartPartETag(int partNumber, String etag) {
        this.partNumber = partNumber;
        this.etag = etag;
    }

    public int getPartNumber() {
        return partNumber;
    }

    public String getEtag() {
        return etag;
    }
}
