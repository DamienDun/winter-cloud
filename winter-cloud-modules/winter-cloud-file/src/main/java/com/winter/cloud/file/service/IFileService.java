package com.winter.cloud.file.service;

import com.winter.cloud.file.domain.dto.FileDownloadZipInput;
import com.winter.cloud.file.domain.dto.FileDownloadZipSpecifyNameInput;
import com.winter.cloud.file.domain.dto.FileUploadInput;
import com.winter.cloud.file.domain.entity.ResourceFile;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;

/**
 * 文件存储服务
 */
public interface IFileService {

    /**
     * 上传文件（仅文件流，兼容 Feign）
     */
    default String uploadFile(MultipartFile file) throws Exception {
        FileUploadInput input = new FileUploadInput();
        input.setFile(file);
        // Feign/头像等场景不打水印；页面上传走 FileUploadInput.watermark=null 读系统参数
        input.setWatermark(false);
        ResourceFile record = uploadFile(input);
        return record == null ? null : record.getPath();
    }

    /**
     * 上传文件（对齐 portal：支持 encrypt / watermark / module 等）
     *
     * @return 资源记录（含访问地址、MD5、类型等）
     */
    ResourceFile uploadFile(FileUploadInput input) throws Exception;

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