package com.winter.cloud.file.service;

import com.winter.cloud.file.domain.ResourceFile;
import org.springframework.web.multipart.MultipartFile;

/**
 * 资源文件上传记录
 */
public interface IResourceFileService {

    /**
     * 按 MD5 查询
     */
    ResourceFile queryByFileMd5(String fileMd5);

    /**
     * 按访问路径查询
     */
    ResourceFile queryByPath(String path);

    /**
     * 保存或累加上传次数（相同 MD5 不重复占存储时更新次数）
     */
    ResourceFile saveUploadRecord(MultipartFile file, String accessUrl, String relativePath,
                                  String channelId, String bucketName, String fileMd5, Long length);

    /**
     * 按路径删除记录
     */
    void deleteByPath(String path);
}