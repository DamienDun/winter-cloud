package com.winter.cloud.file.service;

import com.winter.cloud.file.domain.dto.FileDownloadZipInput;
import com.winter.cloud.file.domain.dto.FileDownloadZipSpecifyNameInput;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;

/**
 * 文件存储服务
 */
public interface IFileService {

    /**
     * 上传文件
     *
     * @param file 上传的文件
     * @return 访问地址
     */
    String uploadFile(MultipartFile file) throws Exception;

    /**
     * 删除文件
     *
     * @param fileUrl 文件访问URL或对象路径
     */
    void deleteFile(String fileUrl) throws Exception;

    /**
     * 是否存在文件
     */
    default boolean existFile(String path) {
        return false;
    }

    /**
     * 下载文件到响应流
     */
    default void downloadFile(String path, HttpServletResponse response) throws Exception {
        throw new UnsupportedOperationException("当前存储实现不支持下载");
    }

    /**
     * 多个文件打包下载 zip（zip 内文件名取 url 末段）
     */
    void downloadZip(FileDownloadZipInput input, HttpServletResponse response);

    /**
     * 多个文件打包下载 zip（可指定 zip 内文件名）
     */
    void downloadZipSpecifyName(FileDownloadZipSpecifyNameInput input, HttpServletResponse response);
}