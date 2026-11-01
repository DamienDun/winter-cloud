package com.winter.cloud.common.filestorage.clients.huawei;

import com.obs.services.ObsClient;
import com.obs.services.model.*;
import com.winter.cloud.common.core.utils.ExceptionUtil;
import com.winter.cloud.common.core.utils.StringUtils;
import com.winter.cloud.common.core.utils.tuple.TupleTwo;
import com.winter.cloud.common.filestorage.*;
import com.winter.cloud.common.filestorage.multipart.MultipartListedPart;
import com.winter.cloud.common.filestorage.multipart.MultipartPartETag;
import com.winter.cloud.common.filestorage.multipart.MultipartUploadOptions;
import com.winter.cloud.common.filestorage.multipart.MultipartUploadSupport;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.DisposableBean;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 华为云 OBS
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2022/8/17 14:20
 */
public class HuaWeiStorageClient extends AbstractCloudStorageClient<HuaWeiBucket> implements Closeable, DisposableBean, MultipartUploadSupport {

    /**
     * 存储代码
     */
    public static final String CHANNEL_ID = "HuaWeiOBS";

    /**
     * 存储名称
     */
    public static final String CHANNEL_NAME = "华为云对象存储";

    /**
     * Obs客户端
     */
    protected final ObsClient obsClient;

    /**
     * HuaWeiStorageClient
     *
     * @param endpoint
     * @param defaultBucketName
     * @param accessKey
     * @param secretKey
     */
    public HuaWeiStorageClient(String endpoint, String defaultBucketName,
                               String accessKey, String secretKey) {
        super(endpoint, defaultBucketName, accessKey, secretKey);
        this.obsClient = new ObsClient(this.getAccessKey(), this.getSecretKey(), this.getEndpoint());
    }

    /**
     * MinioStorageClient
     *
     * @param properties 属性
     */
    public HuaWeiStorageClient(HuaWeiStorageClientProperties properties) {
        this(properties.getEndpoint(),
                properties.getDefaultBucketName(),
                properties.getAccessKey(),
                properties.getSecretKey());
        this.setStorageClientProperties(properties);
    }

    @Override
    public boolean existBucket(String bucketName) {
        ExceptionUtil.checkNotNullOrBlank(bucketName, "bucketName");
        return this.obsClient.headBucket(bucketName);
    }

    @Override
    public HuaWeiBucket createBucket(String bucketName) {
        ExceptionUtil.checkNotNullOrBlank(bucketName, "bucketName");
        ObsBucket obsBucket = this.obsClient.createBucket(bucketName);
        this.obsClient.setBucketAcl(bucketName, AccessControlList.REST_CANNED_PUBLIC_READ);
        HuaWeiBucket bucket = new HuaWeiBucket(bucketName);
        bucket.setCreationDate(obsBucket.getCreationDate());
        bucket.setLocation(obsBucket.getLocation());
        return bucket;
    }

    @Override
    public HuaWeiBucket getBucket(String bucketName) {
        ExceptionUtil.checkNotNullOrBlank(bucketName, "bucketName");
        if (this.obsClient.headBucket(bucketName)) {
            BucketMetadataInfoResult result = this.obsClient.getBucketMetadata(new BucketMetadataInfoRequest(bucketName));
            HuaWeiBucket bucket = new HuaWeiBucket(bucketName);
            //bucket.setCreationDate(result.getCreationDate());
            bucket.setLocation(result.getLocation());
        }
        return null;
    }

    /**
     * 设置文件选项
     *
     * @param options 选项
     * @param request 请求
     */
    protected void setHeaders(ObjectMetadata options, FileStorageRequest request) {
        String contentType = this.getFileContentType(request.getFileInfo().getExtensionName());
        if (StringUtils.isNotEmpty(contentType)) {
            options.setContentType(contentType);
        }
        String contentDisposition = this.getContentDisposition(request);
        if (StringUtils.isNotEmpty(contentDisposition)) {
            options.getMetadata().put(CONTENT_DISPOSITION, contentDisposition);
        }
    }

    @Override
    public FileObject saveFile(FileStorageRequest request) throws Exception {
        ExceptionUtil.checkNotNull(request, "request");
        String bucketName = this.checkBucketName(request.getBucketName());
        if (!this.obsClient.headBucket(bucketName)) {
            this.obsClient.createBucket(bucketName);
            this.obsClient.setBucketAcl(bucketName, AccessControlList.REST_CANNED_PUBLIC_READ);
        }
        InputStream inputStream = null;
        try {
            TupleTwo<Long, InputStream> two = this.readRequestStream(request);
            long size = two.getItem1();
            inputStream = two.getItem2();
            ObjectMetadata options = new ObjectMetadata();
            options.setContentLength(size);
            this.setHeaders(options, request);
            PutObjectResult objectResult = this.obsClient.putObject(bucketName, request.getFileInfo().getFullPath(), inputStream, options);
            FileObject fileObject = new FileObject();
            fileObject.setFileInfo(request.getFileInfo());
            fileObject.getFileInfo().setLength(size);
            fileObject.setUrl(request.getFileInfo().getFullPath());
            fileObject.setAccessUrl(objectResult.getObjectUrl());
            return fileObject;
        } finally {
            IOUtils.closeQuietly(request.getInputStream());
            IOUtils.closeQuietly(inputStream);
        }
    }

    @Override
    public FileStorageObject getFile(String bucketName, String fullPath) {
        bucketName = this.checkBucketName(bucketName);
        ExceptionUtil.checkNotNullOrBlank(fullPath, "fullPath");
        FileInfo fileInfo = new FileInfo(fullPath, true);
        ObsObject obsObject = this.obsClient.getObject(bucketName, fileInfo.getFullPath());
        if (obsObject == null) {
            return null;
        }
        FileStorageObject fileStorageObject = new FileStorageObject();
        fileStorageObject.setFileInfo(new FileInfo(fullPath, true));
        fileStorageObject.setAccessUrl(this.getAccessUrl(bucketName, fileInfo));
        fileStorageObject.getFileInfo().setLength(obsObject.getMetadata().getContentLength());
        fileStorageObject.setUrl(fileInfo.getFullPath());
        fileStorageObject.setInputStream(obsObject.getObjectContent());
        return fileStorageObject;
    }

    @Override
    public boolean existFile(String bucketName, String fullPath) {
        bucketName = this.checkBucketName(bucketName);
        ExceptionUtil.checkNotNullOrBlank(fullPath, "fullPath");
        FileInfo fileInfo = new FileInfo(fullPath, true);
        return this.obsClient.doesObjectExist(bucketName, fileInfo.getFullPath());
    }

    @Override
    public void deleteFile(String bucketName, String fullPath) {
        bucketName = this.checkBucketName(bucketName);
        ExceptionUtil.checkNotNullOrBlank(fullPath, "fullPath");
        FileInfo fileInfo = new FileInfo(fullPath, true);
        this.obsClient.deleteObject(bucketName, fileInfo.getFullPath());
    }

    @Override
    public List<FileObject> listFileObjects(String bucketName, String prefix) {
        bucketName = this.checkBucketName(bucketName);
        ListObjectsRequest objectsRequest = new ListObjectsRequest(bucketName);
        objectsRequest.setPrefix(prefix);
        ObjectListing listing = this.obsClient.listObjects(objectsRequest);
        if (listing.getObjects() == null) {
            return new ArrayList<>();
        }
        List<FileObject> fileObjects = new ArrayList<>(listing.getObjects().size());
        for (ObsObject object : listing.getObjects()) {
            FileObject fileObject = new FileObject();
            FileInfo fileInfo = new FileInfo(object.getObjectKey(),
                    object.getMetadata().getContentLength() != null
                            && object.getMetadata().getContentLength() > 0);
            fileInfo.setLength(object.getMetadata().getContentLength());
            fileObject.setFileInfo(fileInfo);
            fileObject.setUrl(object.getObjectKey());
            fileObject.setAccessUrl(this.getAccessUrl(bucketName, object.getObjectKey()));
            fileObjects.add(fileObject);
        }
        return fileObjects;
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
    public String toString() {
        return "启用 FileStorage " + this.getChannelName()
                + " 默认分区:" + this.getDefaultBucketName()
                + " 访问根路径:" + this.getEndpoint();
    }

    @Override
    public MultipartUploadOptions initiateMultipartUpload(String bucketName, String objectName,
                                                          long objectSize, long partSize, String contentType) {
        if (!this.existBucket(bucketName)) {
            this.createBucket(bucketName);
        }
        InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(bucketName, objectName);
        if (StringUtils.isNotEmpty(contentType)) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(contentType);
            request.setMetadata(metadata);
        }
        InitiateMultipartUploadResult result = this.obsClient.initiateMultipartUpload(request);
        return new MultipartUploadOptions(CHANNEL_ID, bucketName, objectName, result.getUploadId(),
                objectSize, partSize, contentType, null);
    }

    @Override
    public MultipartPartETag uploadPart(MultipartUploadOptions options, byte[] data, int partNumber, int availableSize) {
        UploadPartRequest request = new UploadPartRequest(options.getBucketName(), options.getObjectName());
        request.setUploadId(options.getUploadId());
        request.setPartNumber(partNumber);
        request.setInput(new ByteArrayInputStream(data, 0, availableSize));
        request.setPartSize((long) availableSize);
        UploadPartResult result = this.obsClient.uploadPart(request);
        return new MultipartPartETag(partNumber, result.getEtag());
    }

    @Override
    public List<MultipartListedPart> listMultipartParts(String bucketName, String objectName, String uploadId) {
        ListPartsRequest request = new ListPartsRequest(bucketName, objectName, uploadId);
        ListPartsResult result = this.obsClient.listParts(request);
        List<MultipartListedPart> parts = new ArrayList<>();
        if (result.getMultipartList() != null) {
            for (Multipart multipart : result.getMultipartList()) {
                parts.add(new MultipartListedPart(multipart.getPartNumber(), multipart.getEtag(), multipart.getSize()));
            }
        }
        return parts;
    }

    @Override
    public void completeMultipartUpload(String bucketName, String objectName, String uploadId,
                                        List<MultipartPartETag> parts) {
        // 华为 OBS PartEtag 构造参数顺序为 (etag, partNumber)
        List<PartEtag> partETags = parts.stream()
                .sorted(Comparator.comparingInt(MultipartPartETag::getPartNumber))
                .map(part -> new PartEtag(part.getEtag(), part.getPartNumber()))
                .collect(Collectors.toList());
        CompleteMultipartUploadRequest request = new CompleteMultipartUploadRequest(bucketName, objectName, uploadId, partETags);
        this.obsClient.completeMultipartUpload(request);
    }

    @Override
    public void abortMultipartUpload(String bucketName, String objectName, String uploadId) {
        this.obsClient.abortMultipartUpload(new AbortMultipartUploadRequest(bucketName, objectName, uploadId));
    }

    @Override
    public void destroy() throws Exception {
        this.close();
    }

    @Override
    public void close() throws IOException {
        this.obsClient.close();
    }
}
