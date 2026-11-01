package com.winter.cloud.common.filestorage.clients.aliyun;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSBuilder;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.model.*;
import com.winter.cloud.common.core.utils.ExceptionUtil;
import com.winter.cloud.common.core.utils.StringUtils;
import com.winter.cloud.common.filestorage.*;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.DisposableBean;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;

/**
 * 阿里云存储客户端
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2022/8/17 14:14
 */
public class AliyunStorageClient extends AbstractCloudStorageClient<AliyunBucket> implements Closeable, DisposableBean {

    /**
     * 存储代码
     */
    public static final String CHANNEL_ID = "aliyunOOS";

    /**
     * 存储名称
     */
    public static final String CHANNEL_NAME = "阿里云对象存储";

    protected final OSS oss;
    private CannedAccessControlList controlList = CannedAccessControlList.PublicRead;

    /**
     * AliyunStorageClient
     *
     * @param properties 属性
     */
    public AliyunStorageClient(AliyunStorageClientProperties properties) {
        this(properties.getEndpoint(),
                properties.getDefaultBucketName(),
                properties.getAccessKey(),
                properties.getSecretKey(),
                properties.toCannedAccessControlList());
        this.setStorageClientProperties(properties);
    }

    /**
     * 实例化 AliyunStorageClient
     *
     * @param endpoint
     * @param defaultBucketName
     * @param accessKey
     * @param secretKey
     */
    public AliyunStorageClient(String endpoint, String defaultBucketName,
                               String accessKey,
                               String secretKey) {
        this(endpoint, defaultBucketName, accessKey, secretKey, CannedAccessControlList.PublicRead);
    }

    /**
     * 实例化 AliyunStorageClient
     *
     * @param endpoint
     * @param defaultBucketName
     * @param accessKey
     * @param secretKey
     * @param controlList
     */
    public AliyunStorageClient(String endpoint,
                               String defaultBucketName,
                               String accessKey,
                               String secretKey,
                               CannedAccessControlList controlList) {
        super(endpoint, defaultBucketName, accessKey, secretKey);
        DefaultCredentialProvider credsProvider = new DefaultCredentialProvider(
                ExceptionUtil.checkNotNullOrBlank(accessKey, "accessKey"),
                ExceptionUtil.checkNotNullOrBlank(secretKey, "secretKey"));
        OSSBuilder ossBuilder = new OSSClientBuilder();
        this.oss = ossBuilder.build(this.getEndpoint(), credsProvider);
        this.setControlList(controlList);
    }

    @Override
    public String getChannelId() {
        return CHANNEL_ID;
    }

    @Override
    public String getChannelName() {
        return CHANNEL_NAME;
    }

    /**
     * @return
     */
    public CannedAccessControlList getControlList() {
        return controlList;
    }

    /**
     * @param controlList
     */
    public void setControlList(CannedAccessControlList controlList) {
        if (controlList == null) {
            controlList = CannedAccessControlList.Default;
        }
        this.controlList = controlList;
    }


    @Override
    public boolean existBucket(String bucketName) {
        ExceptionUtil.checkNotNullOrBlank(bucketName, "bucketName");
        return this.oss.doesBucketExist(bucketName);
    }

    private AliyunBucket createBucket(Bucket bucket) {
        AliyunBucket partition = new AliyunBucket(bucket.getName());
        partition.setCreationDate(bucket.getCreationDate());
        partition.setExtranetEndpoint(bucket.getExtranetEndpoint());
        partition.setIntranetEndpoint(bucket.getIntranetEndpoint());
        partition.setLocation(bucket.getLocation());
        return partition;
    }

    @Override
    public AliyunBucket createBucket(String bucketName) {
        ExceptionUtil.checkNotNullOrBlank(bucketName, "bucketName");
        Bucket bucket = this.oss.createBucket(bucketName);
        this.oss.setBucketAcl(bucketName, this.getControlList());
        if (bucket != null) {
            return this.createBucket(bucket);
        }
        return null;
    }

    @Override
    public AliyunBucket getBucket(String bucketName) {
        ExceptionUtil.checkNotNullOrBlank(bucketName, "bucketName");
        if (this.oss.doesBucketExist(bucketName)) {
            BucketInfo bucketInfo = this.oss.getBucketInfo(bucketName);
            if (bucketInfo != null) {
                return this.createBucket(bucketInfo.getBucket());
            }
        }
        return null;
    }

    /**
     * 设置文件选项
     *
     * @param metadata 选项
     * @param request  请求
     */
    protected void setHeaders(ObjectMetadata metadata, FileStorageRequest request) {
        String contentType = this.getFileContentType(request.getFileInfo().getExtensionName());
        if (StringUtils.isNotEmpty(contentType)) {
            metadata.setContentType(contentType);
        }
        String contentDisposition = this.getContentDisposition(request);
        if (StringUtils.isNotEmpty(contentDisposition)) {
            metadata.setHeader(CONTENT_DISPOSITION, contentDisposition);
        }
    }


    @Override
    public FileObject saveFile(FileStorageRequest request) {
        ExceptionUtil.checkNotNull(request, "request");
        try {
            String bucketName = this.checkBucketName(request.getBucketName());
            if (!this.oss.doesBucketExist(bucketName)) {
                this.oss.createBucket(bucketName);
                this.oss.setBucketAcl(bucketName, this.getControlList());
                this.getLogger().info("创建阿里云 bucket：" + bucketName + " 权限：" + this.getControlList());
            }
            ObjectMetadata metadata = new ObjectMetadata();
            this.setHeaders(metadata, request);
            this.oss.putObject(bucketName, request.getFileInfo().getFullPath(), request.getInputStream(), metadata);
            FileObject fileObject = new FileObject();
            fileObject.setFileInfo(request.getFileInfo());
            if (request.getFileInfo().getLength() > 0L) {
                fileObject.getFileInfo().setLength(request.getFileInfo().getLength());
            } else {
                OSSObject ossObject = this.oss.getObject(bucketName, request.getFileInfo().getFullPath());
                IOUtils.closeQuietly(ossObject.getObjectContent());
                fileObject.getFileInfo().setLength(ossObject.getObjectMetadata().getContentLength());
            }
            fileObject.setUrl(request.getFileInfo().getFullPath());
            fileObject.setAccessUrl(this.getAccessUrl(bucketName, request.getFileInfo()));
            return fileObject;
        } finally {
            IOUtils.closeQuietly(request.getInputStream());
        }
    }

    @Override
    public FileStorageObject getFile(String bucketName, String fullPath) {
        FileInfo fileInfo = new FileInfo(fullPath, true);
        bucketName = this.checkBucketName(bucketName);
        OSSObject ossObject = this.oss.getObject(bucketName, fileInfo.getFullPath());
        if (ossObject == null) {
            return null;
        }
        FileStorageObject fileStorageObject = new FileStorageObject();
        fileStorageObject.setFileInfo(new FileInfo(fullPath, true));
        fileStorageObject.setAccessUrl(this.getAccessUrl(bucketName, fileInfo));
        fileStorageObject.getFileInfo().setLength(ossObject.getObjectMetadata().getContentLength());
        fileStorageObject.setUrl(fileInfo.getFullPath());
        fileStorageObject.setInputStream(ossObject.getObjectContent());
        return fileStorageObject;
    }

    @Override
    public boolean existFile(String bucketName, String fullPath) {
        bucketName = this.checkBucketName(bucketName);
        FileInfo fileInfo = new FileInfo(fullPath, true);
        return this.oss.doesObjectExist(bucketName, fileInfo.getFullPath());
    }

    @Override
    public void deleteFile(String bucketName, String fullPath) {
        bucketName = this.checkBucketName(bucketName);
        FileInfo fileInfo = new FileInfo(fullPath, true);
        this.oss.deleteObject(this.checkBucketName(bucketName), fileInfo.getFullPath());
    }

    @Override
    public List<FileObject> listFileObjects(String bucketName, String prefix) {
        bucketName = this.checkBucketName(bucketName);
        if (prefix != null && "".equals(prefix.trim())) {
            prefix = null;
        }
        ObjectListing objectListing = this.oss.listObjects(this.checkBucketName(bucketName), prefix);
        List<FileObject> fileObjects = new ArrayList<>();
        if (objectListing.getObjectSummaries() != null) {
            for (OSSObjectSummary oosSummary : objectListing.getObjectSummaries()) {
                FileObject fileObject = new FileObject();
                FileInfo fileInfo = new FileInfo(oosSummary.getKey(), true, oosSummary.getSize());
                fileObject.setFileInfo(fileInfo);
                fileObject.setUrl(fileInfo.getFullPath());
                fileObject.setAccessUrl(this.getAccessUrl(bucketName, fileInfo));
                fileObjects.add(fileObject);
            }
        }
        return fileObjects;
    }

    @Override
    public void destroy() throws Exception {
        this.close();
    }

    @Override
    public void close() {
        this.oss.shutdown();
    }
}
