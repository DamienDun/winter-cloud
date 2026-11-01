package com.winter.cloud.common.filestorage.clients.aws;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AWS S3 对象存储客户端
 * <p>
 * 采用 AWS S3 Java SDK，支持 S3 兼容对象存储，开启 path-style，关闭分块编码以减少兼容偏差
 * </p>
 *
 * @author Damien
 * @description
 * @create 2026/7/27
 */
public class AwsStorageClient extends AbstractStorageClient<AwsBucket> implements Closeable, DisposableBean, MultipartUploadSupport {

    /**
     * 存储代码
     */
    public static final String CHANNEL_ID = "Aws";

    /**
     * 存储名称
     */
    public static final String CHANNEL_NAME = "AWS S3 对象存储";

    private final String serverUrl;
    private final String accessKey;
    private final String secretKey;
    private final String region;
    private final boolean pathStyleAccess;

    /**
     * AWS S3 客户端
     */
    protected final AmazonS3 amazonS3;

    /**
     * AwsStorageClient
     *
     * @param endpoint          访问根路径
     * @param defaultBucketName 默认分区
     * @param serverUrl         S3 Endpoint
     * @param accessKey         访问键
     * @param secretKey         密钥
     * @param region            区域
     * @param pathStyleAccess   是否 path-style
     */
    public AwsStorageClient(String endpoint, String defaultBucketName, String serverUrl,
                            String accessKey, String secretKey, String region, boolean pathStyleAccess) {
        super(endpoint, defaultBucketName);
        this.serverUrl = serverUrl;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.region = StringUtils.isNotEmpty(region) ? region : "us-east-1";
        this.pathStyleAccess = pathStyleAccess;
        this.amazonS3 = this.createAmazonS3();
    }

    /**
     * AwsStorageClient
     *
     * @param properties 属性
     */
    public AwsStorageClient(AwsStorageClientProperties properties) {
        this(properties.getEndpoint(),
                properties.getDefaultBucketName(),
                properties.getServerUrl(),
                properties.getAccessKey(),
                properties.getSecretKey(),
                properties.getRegion(),
                properties.isPathStyleAccess());
        this.setStorageClientProperties(properties);
    }

    /**
     * 创建 AWS S3 客户端
     *
     * @return AmazonS3
     */
    private AmazonS3 createAmazonS3() {
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        // S3 兼容场景常用 V4 签名
        clientConfiguration.setSignerOverride("AWSS3V4SignerType");
        return AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(
                        new BasicAWSCredentials(this.getAccessKey(), this.getSecretKey())))
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(
                        this.getServerUrl(), this.getRegion()))
                .withPathStyleAccessEnabled(this.isPathStyleAccess())
                .withClientConfiguration(clientConfiguration)
                // 关闭 chunked 编码，避免部分 S3 兼容网关签名校验失败
                .disableChunkedEncoding()
                .build();
    }

    @Override
    public boolean existBucket(String bucketName) {
        ExceptionUtil.checkNotNullOrBlank(bucketName, "bucketName");
        return this.amazonS3.doesBucketExistV2(bucketName);
    }

    @Override
    public AwsBucket createBucket(String bucketName) {
        ExceptionUtil.checkNotNullOrBlank(bucketName, "bucketName");
        this.amazonS3.createBucket(bucketName);
        return new AwsBucket(bucketName);
    }

    @Override
    public AwsBucket getBucket(String bucketName) {
        ExceptionUtil.checkNotNullOrBlank(bucketName, "bucketName");
        if (this.existBucket(bucketName)) {
            return new AwsBucket(bucketName);
        }
        return null;
    }

    /**
     * 设置文件元数据
     *
     * @param metadata 元数据
     * @param request  请求
     * @param size     内容长度
     */
    protected void setHeaders(ObjectMetadata metadata, FileStorageRequest request, long size) {
        metadata.setContentLength(size);
        String contentType = this.getFileContentType(request.getFileInfo().getExtensionName());
        if (StringUtils.isNotEmpty(contentType)) {
            metadata.setContentType(contentType);
        }
        String contentDisposition = this.getContentDisposition(request);
        if (StringUtils.isNotEmpty(contentDisposition)) {
            metadata.setContentDisposition(contentDisposition);
        }
    }

    @Override
    public FileObject saveFile(FileStorageRequest request) throws Exception {
        ExceptionUtil.checkNotNull(request, "request");
        String bucketName = this.checkBucketName(request.getBucketName());
        if (!this.existBucket(bucketName)) {
            this.createBucket(bucketName);
        }
        InputStream inputStream = null;
        try {
            TupleTwo<Long, InputStream> two = this.readRequestStream(request);
            long size = two.getItem1();
            inputStream = two.getItem2();
            ObjectMetadata metadata = new ObjectMetadata();
            this.setHeaders(metadata, request, size);
            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    bucketName, request.getFileInfo().getFullPath(), inputStream, metadata);
            this.amazonS3.putObject(putObjectRequest);
            FileObject fileObject = new FileObject();
            fileObject.setFileInfo(request.getFileInfo());
            fileObject.getFileInfo().setLength(size);
            fileObject.setUrl(request.getFileInfo().getFullPath());
            fileObject.setAccessUrl(this.getAccessUrl(bucketName, request.getFileInfo().getFullPath()));
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
        try {
            S3Object s3Object = this.amazonS3.getObject(bucketName, fileInfo.getFullPath());
            if (s3Object == null) {
                return null;
            }
            FileStorageObject fileStorageObject = new FileStorageObject();
            fileStorageObject.setFileInfo(fileInfo);
            if (s3Object.getObjectMetadata() != null) {
                fileInfo.setLength(s3Object.getObjectMetadata().getContentLength());
            }
            fileStorageObject.setAccessUrl(this.getAccessUrl(bucketName, fullPath));
            fileStorageObject.setUrl(fileInfo.getFullPath());
            fileStorageObject.setInputStream(s3Object.getObjectContent());
            return fileStorageObject;
        } catch (AmazonS3Exception e) {
            if (e.getStatusCode() == 404) {
                return null;
            }
            throw e;
        }
    }

    @Override
    public boolean existFile(String bucketName, String fullPath) {
        bucketName = this.checkBucketName(bucketName);
        ExceptionUtil.checkNotNullOrBlank(fullPath, "fullPath");
        FileInfo fileInfo = new FileInfo(fullPath, true);
        return this.amazonS3.doesObjectExist(bucketName, fileInfo.getFullPath());
    }

    /**
     * 获取访问 Url
     *
     * @param bucketName 分区名称
     * @param fileInfo   文件信息
     * @return 访问地址
     */
    public String getAccessUrl(String bucketName, FileInfo fileInfo) {
        // 优先使用配置的 endpoint 拼路径；若未配置则回退 serverUrl
        if (StringUtils.isNotEmpty(this.getEndpoint())) {
            return this.getPathAddress(this.getEndpoint(), bucketName, fileInfo.getFullPath());
        }
        return this.getPathAddress(this.getServerUrl(), bucketName, fileInfo.getFullPath());
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
        FileInfo fileInfo = new FileInfo(fullPath, true);
        this.amazonS3.deleteObject(bucketName, fileInfo.getFullPath());
    }

    @Override
    public List<FileObject> listFileObjects(String bucketName, String prefix) {
        bucketName = this.checkBucketName(bucketName);
        ObjectListing listing = this.amazonS3.listObjects(bucketName, prefix);
        List<S3ObjectSummary> summaries = listing.getObjectSummaries();
        if (summaries == null || summaries.isEmpty()) {
            return new ArrayList<>();
        }
        List<FileObject> fileObjects = new ArrayList<>(summaries.size());
        for (S3ObjectSummary summary : summaries) {
            FileObject fileObject = new FileObject();
            FileInfo fileInfo = new FileInfo(summary.getKey(), summary.getSize() > 0);
            fileInfo.setLength(summary.getSize());
            fileObject.setFileInfo(fileInfo);
            fileObject.setUrl(summary.getKey());
            fileObject.setAccessUrl(this.getAccessUrl(bucketName, summary.getKey()));
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

    public String getServerUrl() {
        return this.serverUrl;
    }

    public String getAccessKey() {
        return this.accessKey;
    }

    public String getSecretKey() {
        return this.secretKey;
    }

    public String getRegion() {
        return this.region;
    }

    public boolean isPathStyleAccess() {
        return this.pathStyleAccess;
    }

    /**
     * 获取底层 AmazonS3 客户端
     *
     * @return AmazonS3
     */
    public AmazonS3 getAmazonS3() {
        return this.amazonS3;
    }

    @Override
    public String toString() {
        return "启用 FileStorage " + this.getChannelName()
                + " 默认分区:" + this.getDefaultBucketName()
                + " 访问根路径:" + this.getEndpoint()
                + " 服务器地址:" + this.getServerUrl()
                + " 区域:" + this.getRegion();
    }

    @Override
    public MultipartUploadOptions initiateMultipartUpload(String bucketName, String objectName,
                                                          long objectSize, long partSize, String contentType) {
        if (!this.existBucket(bucketName)) {
            this.createBucket(bucketName);
        }
        ObjectMetadata metadata = new ObjectMetadata();
        if (StringUtils.isNotEmpty(contentType)) {
            metadata.setContentType(contentType);
        }
        InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(bucketName, objectName, metadata);
        InitiateMultipartUploadResult result = this.amazonS3.initiateMultipartUpload(request);
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
        UploadPartResult result = this.amazonS3.uploadPart(request);
        return new MultipartPartETag(partNumber, result.getETag());
    }

    @Override
    public List<MultipartListedPart> listMultipartParts(String bucketName, String objectName, String uploadId) {
        PartListing listing = this.amazonS3.listParts(new ListPartsRequest(bucketName, objectName, uploadId));
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
        this.amazonS3.completeMultipartUpload(new CompleteMultipartUploadRequest(bucketName, objectName, uploadId, partETags));
    }

    @Override
    public void abortMultipartUpload(String bucketName, String objectName, String uploadId) {
        this.amazonS3.abortMultipartUpload(new AbortMultipartUploadRequest(bucketName, objectName, uploadId));
    }

    @Override
    public void destroy() {
        this.close();
    }

    @Override
    public void close() {
        if (this.amazonS3 != null) {
            this.amazonS3.shutdown();
        }
    }
}
