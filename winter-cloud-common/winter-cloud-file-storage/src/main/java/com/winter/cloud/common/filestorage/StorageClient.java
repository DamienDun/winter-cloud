package com.winter.cloud.common.filestorage;

import com.winter.cloud.common.filestorage.support.channel.Channel;
import java.io.InputStream;
import java.util.List;

/**
 * 存储客户端
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2022/8/15 20:46
 */
public interface StorageClient extends Channel {

    /**
     * 获取终结点，即如根url地址
     *
     * @return
     */
    String getEndpoint();

    /**
     * 获取默认分区名称
     *
     * @return
     */
    String getDefaultBucketName();

    /**
     * 获取读取块大小
     *
     * @return
     */
    int getReadBlockSize();

    /**
     * 设置读块大小
     *
     * @param readBlockSize 读块大小
     */
    void setReadBlockSize(int readBlockSize);

    /**
     * 获取写块大小
     *
     * @return
     */
    int getWriteBlockSize();

    /**
     * 设置写块大小
     *
     * @param writeBlockSize 写块大小
     */
    void setWriteBlockSize(int writeBlockSize);

    /**
     * 是否存在分区
     *
     * @param bucketName 分区名称 或 aliyun oos的 bucketName
     * @return
     */
    boolean existBucket(String bucketName);

    /**
     * 创建分区
     *
     * @param bucketName 分区名称 或 aliyun oos的 bucketName
     * @return
     */
    AbstractBucket createBucket(String bucketName);

    /**
     * 获取分区
     *
     * @param bucketName 分区名称 或 aliyun oos的 bucketName
     * @return 不存在则返回 null
     */
    AbstractBucket getBucket(String bucketName);

    /**
     * 保存文件
     *
     * @param bucketName 分区名称 或 aliyun oos的 bucketName
     * @param fullPath   完整的路径，包括文件名称
     * @param input      输入流
     * @return
     * @throws Exception
     */
    FileObject saveFile(String bucketName, String fullPath, InputStream input) throws Exception;

    /**
     * 保存文件
     *
     * @param request 保存请求
     * @return
     * @throws Exception
     */
    FileObject saveFile(FileStorageRequest request) throws Exception;

    /**
     * 获取文件
     *
     * @param bucketName 分区名称 或 aliyun oos的 bucketName
     * @param fullPath   完整的路径，包括文件名称
     * @return
     */
    FileStorageObject getFile(String bucketName, String fullPath);

    /**
     * 是否存在文件
     *
     * @param bucketName 分区名称 或 aliyun oos的 bucketName
     * @param fullPath   完整的路径，包括文件名称
     * @return
     */
    boolean existFile(String bucketName, String fullPath);

    /**
     * 获取访问Url
     *
     * @param bucketName 分区名称 或 aliyun oos的 bucketName
     * @param fullPath   完整的路径，包括文件名称
     * @return
     */
    String getAccessUrl(String bucketName, String fullPath);

    /**
     * 删除文件
     *
     * @param bucketName 分区名称 或 aliyun oos的 bucketName
     * @param fullPath   完整的路径，包括文件名称
     */
    void deleteFile(String bucketName, String fullPath);

    /**
     * 从分区获取文件列表
     *
     * @param bucketName 分区名称 或 aliyun oos的 bucketName
     * @return
     */
    List<FileObject> listFileObjects(String bucketName);

    /**
     * 获取文件列表
     *
     * @param bucketName 分区名称 或 aliyun oos的 bucketName
     * @param prefix     前缀
     * @return
     */
    List<FileObject> listFileObjects(String bucketName, String prefix);
}
