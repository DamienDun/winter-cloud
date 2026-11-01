package com.winter.cloud.common.filestorage.clients.tencent;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.model.*;
import com.winter.cloud.common.core.utils.ExceptionUtil;
import com.winter.cloud.common.core.utils.StringUtils;
import com.winter.cloud.common.core.utils.tuple.TupleTwo;
import com.winter.cloud.common.filestorage.*;
import com.winter.cloud.common.filestorage.multipart.MultipartListedPart;
import com.winter.cloud.common.filestorage.multipart.MultipartPartETag;
import com.winter.cloud.common.filestorage.multipart.MultipartUploadOptions;
import com.winter.cloud.common.filestorage.multipart.MultipartUploadSupport;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 腾讯云 cos
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2022/8/17 14:26
 */
public class TencentStorageClient extends AbstractCloudStorageClient<TencentBucket> implements MultipartUploadSupport {

    /**
     * 存储代码
     */
    public static final String CHANNEL_ID = "TencentCOS";

    /**
     * 存储名称
     */
    public static final String CHANNEL_NAME = "腾讯云对象存储";

    /**
     * Cos 客户端
     */
    protected final COSClient cosClient;

    /**
     * TencentStorageClient
     *
     * @param endpoint
     * @param defaultBucketName
     * @param accessKey
     * @param secretKey
     */
    public TencentStorageClient(String endpoint, String defaultBucketName,
                                String accessKey, String secretKey) {
        super(StringUtils.removeALLWhitespace(endpoint), defaultBucketName, accessKey, secretKey);
        COSCredentials cred = new BasicCOSCredentials(this.getAccessKey(), this.getSecretKey());
        ClientConfig clientConfig = new ClientConfig();
        this.cosClient = new COSClient(cred, clientConfig);
    }

    /**
     * TencentStorageClient
     *
     * @param properties 属性
     */
    public TencentStorageClient(TencentStorageClientProperties properties) {
        this(properties.getEndpoint(),
                properties.getDefaultBucketName(),
                properties.getAccessKey(),
                properties.getSecretKey());
        this.setStorageClientProperties(properties);
    }

    @Override
    public boolean existBucket(String bucketName) {
        ExceptionUtil.checkNotNullOrBlank(bucketName, "bucketName");
        return this.cosClient.doesBucketExist(bucketName);
    }

    @Override
    public TencentBucket createBucket(String bucketName) {
        ExceptionUtil.checkNotNullOrBlank(bucketName, "bucketName");
        Bucket bucket = this.cosClient.createBucket(bucketName);
        this.cosClient.setBucketAcl(bucketName, CannedAccessControlList.PublicRead);
        TencentBucket tencentBucket = new TencentBucket(bucketName);
        bucket.setCreationDate(bucket.getCreationDate());
        bucket.setLocation(bucket.getLocation());
        return tencentBucket;
    }

    @Override
    public TencentBucket getBucket(String bucketName) {
        ExceptionUtil.checkNotNullOrBlank(bucketName, "bucketName");
        if (this.cosClient.doesBucketExist(bucketName)) {
            String location = this.cosClient.getBucketLocation(bucketName);
            TencentBucket bucket = new TencentBucket(bucketName);
            //bucket.setCreationDate(result.getCreationDate());
            bucket.setLocation(location);
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
            options.setHeader(CONTENT_DISPOSITION, contentDisposition);
        }
    }

    @Override
    public FileObject saveFile(FileStorageRequest request) throws Exception {
        ExceptionUtil.checkNotNull(request, "request");
        String bucketName = this.checkBucketName(request.getBucketName());
        if (!this.cosClient.doesBucketExist(bucketName)) {
            this.cosClient.createBucket(bucketName);
            this.cosClient.setBucketAcl(bucketName, CannedAccessControlList.PublicRead);
        }
        InputStream inputStream = null;
        try {
            TupleTwo<Long, InputStream> two = this.readRequestStream(request);
            long size = two.getItem1();
            inputStream = two.getItem2();
            ObjectMetadata options = new ObjectMetadata();
            options.setContentLength(size);
            this.setHeaders(options, request);
            PutObjectResult objectResult = this.cosClient.putObject(bucketName, request.getFileInfo().getFullPath(), inputStream, options);
            FileObject fileObject = new FileObject();
            fileObject.setFileInfo(request.getFileInfo());
            fileObject.getFileInfo().setLength(size);
            fileObject.setUrl(request.getFileInfo().getFullPath());
            fileObject.setAccessUrl(this.getAccessUrl(bucketName, request.getFileInfo()));
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
        COSObject cosObject = this.cosClient.getObject(bucketName, fileInfo.getFullPath());
        if (cosObject == null) {
            return null;
        }
        FileStorageObject fileStorageObject = new FileStorageObject();
        fileStorageObject.setFileInfo(new FileInfo(fullPath, true));
        fileStorageObject.setAccessUrl(this.getAccessUrl(bucketName, fileInfo));
        fileStorageObject.getFileInfo().setLength(cosObject.getObjectMetadata().getContentLength());
        fileStorageObject.setUrl(fileInfo.getFullPath());
        fileStorageObject.setInputStream(cosObject.getObjectContent());
        return fileStorageObject;
    }

    @Override
    public boolean existFile(String bucketName, String fullPath) {
        bucketName = this.checkBucketName(bucketName);
        ExceptionUtil.checkNotNullOrBlank(fullPath, "fullPath");
        FileInfo fileInfo = new FileInfo(fullPath, true);
        return this.cosClient.doesObjectExist(bucketName, fileInfo.getFullPath());
    }

    @Override
    public void deleteFile(String bucketName, String fullPath) {
        bucketName = this.checkBucketName(bucketName);
        ExceptionUtil.checkNotNullOrBlank(fullPath, "fullPath");
        FileInfo fileInfo = new FileInfo(fullPath, true);
        this.cosClient.deleteObject(bucketName, fileInfo.getFullPath());
    }

    @Override
    public List<FileObject> listFileObjects(String bucketName, String prefix) {
        bucketName = this.checkBucketName(bucketName);
        ObjectListing listing = this.cosClient.listObjects(bucketName, prefix);
        if (listing.getObjectSummaries() == null) {
            return new ArrayList<>();
        }
        List<FileObject> fileObjects = new ArrayList<>(listing.getObjectSummaries().size());
        for (COSObjectSummary summary : listing.getObjectSummaries()) {
            FileObject fileObject = new FileObject();
            FileInfo fileInfo = new FileInfo(summary.getKey(),
                    summary.getSize() > 0L);
            fileInfo.setLength(summary.getSize());
            fileObject.setFileInfo(fileInfo);
            fileObject.setUrl(summary.getKey());
            fileObject.setAccessUrl(this.getAccessUrl(bucketName, summary.getKey()));
            fileObjects.add(fileObject);
        }
        return fileObjects;
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
            request.setObjectMetadata(metadata);
        }
        InitiateMultipartUploadResult result = this.cosClient.initiateMultipartUpload(request);
        return new MultipartUploadOptions(CHANNEL_ID, bucketName, objectName, result.getUploadId(),
                objectSize, partSize, contentType, null);
    }

    @Override
    public MultipartPartETag uploadPart(MultipartUploadOptions options, byte[] data, int partNumber, int availableSize) {
        UploadPartRequest request = new UploadPartRequest()
                .withBucketName(options.getBucketName())
                .withKey(options.getObjectName())
                .withUploadId(options.getUploadId())
                .withPartNumber(partNumber)
                .withInputStream(new ByteArrayInputStream(data, 0, availableSize))
                .withPartSize(availableSize);
        UploadPartResult result = this.cosClient.uploadPart(request);
        return new MultipartPartETag(partNumber, result.getETag());
    }

    @Override
    public List<MultipartListedPart> listMultipartParts(String bucketName, String objectName, String uploadId) {
        PartListing listing = this.cosClient.listParts(new ListPartsRequest(bucketName, objectName, uploadId));
        List<MultipartListedPart> parts = new ArrayList<>();
        if (listing.getParts() != null) {
            for (PartSummary summary : listing.getParts()) {
                parts.add(new MultipartListedPart(summary.getPartNumber(), summary.getETag(), summary.getSize()));
            }
        }
        return parts;
    }

    @Override
    public void completeMultipartUpload(String bucketName, String objectName, String uploadId,
                                        List<MultipartPartETag> parts) {
        List<PartETag> partETags = parts.stream()
                .sorted(Comparator.comparingInt(MultipartPartETag::getPartNumber))
                .map(part -> new PartETag(part.getPartNumber(), part.getEtag()))
                .collect(Collectors.toList());
        this.cosClient.completeMultipartUpload(new CompleteMultipartUploadRequest(bucketName, objectName, uploadId, partETags));
    }

    @Override
    public void abortMultipartUpload(String bucketName, String objectName, String uploadId) {
        this.cosClient.abortMultipartUpload(new AbortMultipartUploadRequest(bucketName, objectName, uploadId));
    }

    @Override
    public String getChannelId() {
        return CHANNEL_ID;
    }

    @Override
    public String getChannelName() {
        return CHANNEL_NAME;
    }
}

