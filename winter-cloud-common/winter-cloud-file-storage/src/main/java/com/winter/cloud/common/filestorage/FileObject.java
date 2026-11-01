package com.winter.cloud.common.filestorage;

import java.io.Serializable;

/**
 * 文件保存对象
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2022/8/15 21:25
 */
public class FileObject implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = -2640630510655701011L;

    /**
     *
     */
    private FileInfo fileInfo;
    /**
     * 获取url
     */
    private String url;
    /**
     * 获取访问文件夹
     */
    private String accessUrl;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getAccessUrl() {
        return accessUrl;
    }

    public void setAccessUrl(String accessUrl) {
        this.accessUrl = accessUrl;
    }

    /**
     * 获取文件信息
     *
     * @return
     */
    public FileInfo getFileInfo() {
        return fileInfo;
    }

    /**
     * 设置文件信息
     *
     * @param fileInfo 文件信息
     */
    public void setFileInfo(FileInfo fileInfo) {
        this.fileInfo = fileInfo;
    }

    @Override
    public String toString() {
        return "url = " + this.getUrl() + " accessUrl = " + this.getAccessUrl() + " fileInfo -> " + getFileInfo();
    }
}
