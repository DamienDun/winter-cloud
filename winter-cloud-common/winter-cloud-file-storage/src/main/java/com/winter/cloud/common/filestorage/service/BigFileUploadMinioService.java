package com.winter.cloud.common.filestorage.service;

import com.winter.cloud.common.filestorage.dto.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 大文件上传minio服务
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2023/12/13 9:40
 */
public interface BigFileUploadMinioService {

    /**
     * 分块上传申请
     *
     * @param input 输入
     * @return
     */
    UploadApplyOutput multipartUploadApply(UploadApplyInput input);

    /**
     * 上传(支持并发)
     *
     * @param input 输入
     * @return
     */
    UploadOutput multipartUpload(UploadInput input);

    /**
     * 删除未完成的多部份上传
     *
     * @param input 输入
     */
    void deleteMultipartUpload(UploadOptionsDto input);

    /**
     * 查询多部份状态
     *
     * @param input 输入
     * @return
     */
    UploadStatus queryMultipartStatus(UploadOptionsDto input);

    /**
     * 下载
     *
     * @param bucketName
     * @param path
     * @param request
     * @param response
     */
    void download(String bucketName, String path, HttpServletRequest request, HttpServletResponse response) throws Exception;

    /**
     * 手动合并
     *
     * @param uploadId
     */
    void merge(String uploadId) throws Exception;
}
