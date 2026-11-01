package com.winter.cloud.common.filestorage.service;

import com.winter.cloud.common.filestorage.dto.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 大文件分片上传服务（支持 MinIO / AWS / OSS / OBS / COS）。
 * 通道由入参 channelId 或配置 winter.storage.client.multipart-channel 决定。
 */
public interface BigFileUploadService {

    /**
     * 分块上传申请
     */
    UploadApplyOutput multipartUploadApply(UploadApplyInput input);

    /**
     * 上传分片（支持并发）
     */
    UploadOutput multipartUpload(UploadInput input);

    /**
     * 删除未完成的多部份上传
     */
    void deleteMultipartUpload(UploadOptionsDto input);

    /**
     * 查询多部份状态
     */
    UploadStatus queryMultipartStatus(UploadOptionsDto input);

    /**
     * 下载
     */
    void download(String channelId, String bucketName, String path,
                  HttpServletRequest request, HttpServletResponse response) throws Exception;

    /**
     * 手动合并
     */
    void merge(String uploadId) throws Exception;
}
