package com.winter.cloud.common.filestorage.clients.minio;

import io.minio.messages.Part;

import java.io.Serializable;

/**
 * 部分上传标签
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2022/8/17 13:27
 */
public class PartUploadTag implements Serializable {

    private static final long serialVersionUID = 6084409274025721486L;
    private final int partNumber;
    private final String etag;

    public PartUploadTag(int partNumber, String etag) {
        this.partNumber = partNumber;
        this.etag = etag;
    }

    public int getPartNumber() {
        return partNumber;
    }

    public String getEtag() {
        return etag;
    }

    /**
     * 创建 Part
     *
     * @return
     */
    public Part createPart() {
        return new Part(this.getPartNumber(), this.etag);
    }
}
