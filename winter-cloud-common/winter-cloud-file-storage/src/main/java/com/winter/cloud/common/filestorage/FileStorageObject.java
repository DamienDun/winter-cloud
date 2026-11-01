package com.winter.cloud.common.filestorage;

import java.io.InputStream;

/**
 * 文件存储对象
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2022/8/15 21:24
 */
public class FileStorageObject extends FileObject {

    private static final long serialVersionUID = 2107478581741859300L;

    private InputStream inputStream;

    /**
     * 获取流
     *
     * @return
     */
    public InputStream getInputStream() {
        return inputStream;
    }

    /**
     * 设置流
     *
     * @param inputStream
     */
    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

}
