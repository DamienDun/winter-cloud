package com.winter.cloud.common.filestorage.multipart;

/**
 * 分片上传限制常量。
 */
public final class MultipartUploadConstants {

    /** 单对象最大 5TiB */
    public static final long MAX_OBJECT_SIZE = 5L * 1024 * 1024 * 1024 * 1024;

    /** 单分片最大 5GiB */
    public static final long MAX_PART_SIZE = 5L * 1024 * 1024 * 1024;

    /** 默认分片 10MiB */
    public static final long DEFAULT_PART_BYTES = 10L * 1024 * 1024;

    private MultipartUploadConstants() {
    }
}
