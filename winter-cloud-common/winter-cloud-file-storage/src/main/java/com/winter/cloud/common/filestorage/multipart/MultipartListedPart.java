package com.winter.cloud.common.filestorage.multipart;

import java.io.Serializable;

/**
 * 已上传分片信息（含大小，用于汇总进度）。
 */
public class MultipartListedPart implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int partNumber;
    private final String etag;
    private final long size;

    public MultipartListedPart(int partNumber, String etag, long size) {
        this.partNumber = partNumber;
        this.etag = etag;
        this.size = size;
    }

    public int getPartNumber() {
        return partNumber;
    }

    public String getEtag() {
        return etag;
    }

    public long getSize() {
        return size;
    }
}
