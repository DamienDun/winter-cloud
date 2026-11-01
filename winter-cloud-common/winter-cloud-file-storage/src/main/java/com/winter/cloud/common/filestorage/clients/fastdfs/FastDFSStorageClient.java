package com.winter.cloud.common.filestorage.clients.fastdfs;

import com.winter.cloud.common.filestorage.support.exception.NotSupportException;
import com.winter.cloud.common.core.exception.base.BaseException;
import com.winter.cloud.common.core.utils.ExceptionUtil;
import com.winter.cloud.common.core.utils.StringUtils;
import com.winter.cloud.common.filestorage.*;
import org.apache.commons.io.IOUtils;
import org.csource.common.NameValuePair;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * FastDFS 存储客户端
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2022/8/17 14:03
 */
public class FastDFSStorageClient extends AbstractStorageClient<FastDFSBucket> {

    /**
     * 存储代码
     */
    public static final String CHANNEL_ID = "fastDFS";

    /**
     * 存储名称
     */
    public static final String CHANNEL_NAME = "FastDFS 文件服务器";

    /**
     * 实例化
     *
     * @param endpoint
     * @param defaultBucketName
     */
    public FastDFSStorageClient(String endpoint, String defaultBucketName) {
        super(endpoint, defaultBucketName);
    }

    /**
     * FastDFSStorageClient
     *
     * @param properties 属性
     */
    public FastDFSStorageClient(FastDFSStorageClientProperties properties) {
        this(properties.getEndpoint(), properties.getDefaultBucketName());
        this.setStorageClientProperties(properties);
    }

    @Override
    public String getChannelId() {
        return CHANNEL_ID;
    }

    @Override
    public String getChannelName() {
        return CHANNEL_NAME;
    }

    @Override
    public boolean existBucket(String bucketName) {
        throw new NotSupportException("不支持的操作");
    }

    @Override
    public FastDFSBucket createBucket(String bucketName) {
        throw new NotSupportException("不支持的操作");
    }

    @Override
    public FastDFSBucket getBucket(String bucketName) {
        return null;
    }

    /**
     * 创建存储客户端
     *
     * @return
     */
    private StorageClient createStorageClient() {
        TrackerClient trackerClient = new TrackerClient();
        TrackerServer trackerServer;
        try {
            trackerServer = trackerClient.getConnection();
        } catch (IOException e) {
            throw new BaseException(e, e.getMessage());
        }
        return new StorageClient(trackerServer, null);
    }

    /**
     * 创建文件
     *
     * @param request 请求
     */
    protected NameValuePair[] createHeaders(FileStorageRequest request) {
        List<NameValuePair> pairs = new ArrayList<>(2);
        String contentType = this.getFileContentType(request.getFileInfo().getExtensionName());
        if (StringUtils.isNotEmpty(contentType)) {
            pairs.add(new NameValuePair("Content-Type", contentType));
        }
        String contentDisposition = this.getContentDisposition(request);
        if (StringUtils.isNotEmpty(contentDisposition)) {
            pairs.add(new NameValuePair(CONTENT_DISPOSITION, contentDisposition));
        }
        return pairs.toArray(new NameValuePair[pairs.size()]);
    }

    @Override
    public FileObject saveFile(FileStorageRequest request) throws Exception {
        ExceptionUtil.checkNotNull(request, "request");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            String bucketName = this.checkBucketName(request.getBucketName());
            StorageClient storageClient = this.createStorageClient();
            this.writeOutputStream(request.getInputStream(), outputStream);
            byte[] bytes = outputStream.toByteArray();
            String[] files = storageClient.upload_file(bucketName, bytes,
                    request.getFileInfo().getExtensionName(), this.createHeaders(request));
            if (files == null || files.length < 2) {
                throw new BaseException("上传失败，获取上传文件信息不正确。");
            }
            FileObject fileObject = new FileObject();
            FileInfo fileInfo = new FileInfo(files[1], true, bytes.length);
            fileObject.setAccessUrl(this.getAccessUrl(files[0], fileInfo.getFullPath()));
            fileObject.setUrl(fileInfo.getFullPath());
            fileObject.setFileInfo(fileInfo);
            return fileObject;
        } finally {
            IOUtils.closeQuietly(request.getInputStream());
            IOUtils.closeQuietly(outputStream);
        }
    }

    @Override
    public FileStorageObject getFile(String bucketName, String fullPath) {
        bucketName = this.checkBucketName(bucketName);
        ExceptionUtil.checkNotNullOrBlank(fullPath, "fullPath");
        StorageClient storageClient = this.createStorageClient();
        try {
            byte[] bytes = storageClient.download_file(bucketName, fullPath);
            FileInfo fileInfo = new FileInfo(fullPath, true, bytes.length);
            FileStorageObject storageObject = new FileStorageObject();
            storageObject.setInputStream(new ByteArrayInputStream(bytes));
            storageObject.setAccessUrl(this.getAccessUrl(bucketName, fullPath));
            storageObject.setUrl(fullPath);
            storageObject.setFileInfo(fileInfo);
            return storageObject;
        } catch (Exception e) {
            throw new BaseException(e, e.getMessage());
        }
    }

    @Override
    public boolean existFile(String bucketName, String fullPath) {
        bucketName = this.checkBucketName(bucketName);
        ExceptionUtil.checkNotNullOrBlank(fullPath, "fullPath");
        StorageClient storageClient = this.createStorageClient();
        try {
            FileInfo fileInfo = new FileInfo(fullPath, true);
            return storageClient.query_file_info(bucketName, fileInfo.getFullPath()) != null;
        } catch (Exception e) {
            throw new BaseException(e, e.getMessage());
        }
    }

    /**
     * 获取访问Url
     *
     * @param bucketName 分区名称
     * @param fileInfo
     * @return
     */
    private String getAccessUrl(String bucketName, FileInfo fileInfo) {
        return this.getPathAddress(this.getEndpoint(), bucketName, fileInfo.getFullPath());
    }

    @Override
    public String getAccessUrl(String bucketName, String fullPath) {
        bucketName = this.checkBucketName(bucketName);
        ExceptionUtil.checkNotNullOrBlank(fullPath, "fullPath");
        FileInfo fileInfo = new FileInfo(fullPath, true);
        return this.getAccessUrl(bucketName, fileInfo);
    }

    @Override
    public void deleteFile(String bucketName, String fullPath) {
        bucketName = this.checkBucketName(bucketName);
        ExceptionUtil.checkNotNullOrBlank(fullPath, "fullPath");
        StorageClient storageClient = this.createStorageClient();
        try {
            FileInfo fileInfo = new FileInfo(fullPath, true);
            storageClient.delete_file(bucketName, fileInfo.getFullPath());
        } catch (Exception e) {
            throw new BaseException(e, e.getMessage());
        }
    }

    @Override
    public List<FileObject> listFileObjects(String bucketName, String prefix) {
        throw new NotSupportException("不支持的操作");
    }

}
