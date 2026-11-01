package com.winter.cloud.common.filestorage.clients.minio;

import com.winter.cloud.common.core.exception.ConfigureException;
import com.winter.cloud.common.core.exception.base.BaseException;
import com.winter.cloud.common.core.utils.ExceptionUtil;
import com.winter.cloud.common.core.utils.StringUtils;
import com.winter.cloud.common.core.utils.tuple.TupleTwo;
import com.winter.cloud.common.filestorage.*;
import com.winter.cloud.common.filestorage.multipart.MultipartListedPart;
import com.winter.cloud.common.filestorage.multipart.MultipartPartETag;
import com.winter.cloud.common.filestorage.multipart.MultipartUploadOptions;
import com.winter.cloud.common.filestorage.multipart.MultipartUploadSupport;
import io.minio.ErrorCode;
import io.minio.ObjectStat;
import io.minio.PutObjectOptions;
import io.minio.Result;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InvalidEndpointException;
import io.minio.errors.InvalidPortException;
import io.minio.messages.Item;
import io.minio.messages.ListPartsResult;
import io.minio.messages.Part;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Minio 存储客户端
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2022/8/17 13:21
 */
public class MinioStorageClient extends AbstractStorageClient<MinioBucket> implements MultipartUploadSupport {

    /**
     * 存储代码
     */
    public static final String CHANNEL_ID = "Minio";

    /**
     * 存储名称
     */
    public static final String CHANNEL_NAME = "Minio 文件服务器";

    private final String serverUrl;
    private final String accessKey;
    private final String secretKey;

    /**
     * 获取客户端
     *
     * @return
     */
    public MinioBigClient getMinioClient() {
        return this.minioClient;
    }

    /**
     * 客户端
     */
    protected final MinioBigClient minioClient;

    /**
     * MinioStorageClient
     *
     * @param endpoint
     * @param defaultBucketName
     */
    public MinioStorageClient(String endpoint, String defaultBucketName, String serverUrl,
                              String accessKey, String secretKey) {
        super(endpoint, defaultBucketName);
        this.serverUrl = serverUrl;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.minioClient = this.createMinioClient();
    }

    /**
     * MinioStorageClient
     *
     * @param properties 属性
     */
    public MinioStorageClient(MinioStorageClientProperties properties) {
        this(properties.getEndpoint(),
                properties.getDefaultBucketName(),
                properties.getServerUrl(),
                properties.getAccessKey(),
                properties.getSecretKey());
        this.setStorageClientProperties(properties);
    }

    /**
     * 创建客户端
     *
     * @return
     */
    private MinioBigClient createMinioClient() {
        try {
            return new MinioBigClient(this.getServerUrl(), this.getAccessKey(), this.getSecretKey());
        } catch (InvalidEndpointException e) {
            throw new ConfigureException("无效的服务器地址:" + this.getServerUrl());
        } catch (InvalidPortException e) {
            throw new ConfigureException("无效的服务器端口:" + this.getServerUrl());
        }
    }

    @Override
    public boolean existBucket(String bucketName) {
        ExceptionUtil.checkNotNullOrBlank(bucketName, "bucketName");
        try {
            return this.minioClient.bucketExists(bucketName);
        } catch (Exception e) {
            throw new BaseException(e, e.getMessage());
        }
    }

    @Override
    public MinioBucket createBucket(String bucketName) {
        try {
            this.minioClient.makeBucket(bucketName);
        } catch (Exception e) {
            throw new BaseException(e, e.getMessage());
        }
        return new MinioBucket(bucketName);
    }

    @Override
    public MinioBucket getBucket(String bucketName) {
        if (this.existBucket(bucketName)) {
            return new MinioBucket(bucketName);
        }
        return null;
    }

    /**
     * 设置文件选项
     *
     * @param options 选项
     * @param request 请求
     */
    protected void setHeaders(PutObjectOptions options, FileStorageRequest request) {
        String contentType = this.getFileContentType(request.getFileInfo().getExtensionName());
        if (StringUtils.isNotEmpty(contentType)) {
            options.setContentType(contentType);
        }
        String contentDisposition = this.getContentDisposition(request);
        if (StringUtils.isNotEmpty(contentDisposition)) {
            Map<String, String> headers = new HashMap<>(1);
            headers.put(CONTENT_DISPOSITION, contentDisposition);
            options.setHeaders(headers);
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
            PutObjectOptions options = new PutObjectOptions(size, -1L);
            this.setHeaders(options, request);
            this.minioClient.putObject(bucketName, request.getFileInfo().getFullPath(), inputStream, options);
            //ObjectStat stat = this.minioClient.statObject(bucketName, request.getFileInfo().getFullPath());
            FileObject fileObject = new FileObject();
            fileObject.setFileInfo(request.getFileInfo());
            fileObject.getFileInfo().setLength(size);
            fileObject.setUrl(request.getFileInfo().getFullPath());
            //fileObject.setAccessUrl(this.minioClient.getObjectUrl(bucketName, request.getFileInfo().getFullPath()));
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
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        InputStream inputStream = null;
        try {
            inputStream = this.minioClient.getObject(bucketName, fullPath);
            long size = this.writeOutputStream(inputStream, outputStream);
            FileInfo fileInfo = new FileInfo(fullPath, true, outputStream.size());
            FileStorageObject storageObject = new FileStorageObject();
            storageObject.setInputStream(new ByteArrayInputStream(outputStream.toByteArray()));
            storageObject.setAccessUrl(this.getAccessUrl(bucketName, fullPath));
            storageObject.setUrl(fullPath);
            storageObject.setFileInfo(fileInfo);
            return storageObject;
        } catch (ErrorResponseException e) {
            if (e.errorResponse().errorCode().equals(ErrorCode.NO_SUCH_OBJECT)) {
                return null;
            }
            throw new BaseException(e, e.getMessage());
        } catch (Exception e) {
            throw new BaseException(e, e.getMessage());
        } finally {
            IOUtils.closeQuietly(outputStream);
            IOUtils.closeQuietly(inputStream);
        }

    }

    @Override
    public boolean existFile(String bucketName, String fullPath) {
        bucketName = this.checkBucketName(bucketName);
        ExceptionUtil.checkNotNullOrBlank(fullPath, "fullPath");
        try {
            ObjectStat stat = this.minioClient.statObject(bucketName, fullPath);
            return stat != null;
        } catch (ErrorResponseException e) {
            if (e.errorResponse().errorCode().equals(ErrorCode.NO_SUCH_OBJECT)) {
                return false;
            }
            throw new BaseException(e, e.getMessage());
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
    public String getAccessUrl(String bucketName, FileInfo fileInfo) {
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
        try {
            FileInfo fileInfo = new FileInfo(fullPath, true);
            this.minioClient.removeObject(bucketName, fileInfo.getFullPath());
        } catch (Exception e) {
            throw new BaseException(e, e.getMessage());
        }
    }

    @Override
    public List<FileObject> listFileObjects(String bucketName, String prefix) {
        bucketName = this.checkBucketName(bucketName);
        try {
            Iterable<Result<Item>> items = this.minioClient.listObjects(bucketName, prefix);
            List<FileObject> fileObjects = new ArrayList<>(16);
            for (Result<Item> item : items) {
                Item i = item.get();
                FileObject fileObject = new FileObject();
                FileInfo fileInfo = new FileInfo(i.objectName(), !i.isDir());
                fileInfo.setLength(i.size());
                fileObject.setFileInfo(fileInfo);
                fileObject.setUrl(i.objectName());
                fileObject.setAccessUrl(this.getAccessUrl(bucketName, i.objectName()));
                fileObjects.add(fileObject);
            }
            return fileObjects;
        } catch (Exception e) {
            throw new BaseException(e, e.getMessage());
        }
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
     * 获取服务器址
     *
     * @return
     */
    public String getServerUrl() {
        return this.serverUrl;
    }

    /**
     * 获取访问Key
     *
     * @return
     */
    public String getAccessKey() {
        return this.accessKey;
    }

    /**
     * 获取访问Secret
     *
     * @return
     */
    public String getSecretKey() {
        return this.secretKey;
    }

    @Override
    public MultipartUploadOptions initiateMultipartUpload(String bucketName, String objectName,
                                                          long objectSize, long partSize, String contentType) throws Exception {
        MinioObjectOptions objectOptions = new MinioObjectOptions(objectSize, partSize);
        objectOptions.setContentType(contentType);
        MinioMultiPartUploadOptions options = this.minioClient.createMultipartUploadOptions(bucketName, objectName, objectOptions);
        return new MultipartUploadOptions(CHANNEL_ID, options.getBucketName(), options.getObjectName(), options.getUploadId(),
                options.getObjectSize(), options.getPartSize(), contentType, options.getSsecHeaders());
    }

    @Override
    public MultipartPartETag uploadPart(MultipartUploadOptions options, byte[] data, int partNumber, int availableSize) throws Exception {
        MinioMultiPartUploadOptions minioOptions = new MinioMultiPartUploadOptions(
                options.getBucketName(), options.getObjectName(), options.getUploadId(),
                options.getObjectSize(), options.getPartSize(), options.getHeaders());
        PartUploadTag tag = this.minioClient.multiPartUpload(minioOptions, data, partNumber, availableSize);
        return new MultipartPartETag(tag.getPartNumber(), tag.getEtag());
    }

    @Override
    public List<MultipartListedPart> listMultipartParts(String bucketName, String objectName, String uploadId) throws Exception {
        ListPartsResult result = this.minioClient.listParts(bucketName, objectName, null, null, uploadId);
        if (result == null || result.partList() == null) {
            return new ArrayList<>();
        }
        return result.partList().stream()
                .map(part -> new MultipartListedPart(part.partNumber(), part.etag(), part.partSize()))
                .collect(Collectors.toList());
    }

    @Override
    public void completeMultipartUpload(String bucketName, String objectName, String uploadId,
                                        List<MultipartPartETag> parts) throws Exception {
        List<Part> minioParts = parts.stream()
                .map(part -> new Part(part.getPartNumber(), part.getEtag()))
                .collect(Collectors.toList());
        this.minioClient.completeMultipartUpload(bucketName, objectName, uploadId, minioParts);
    }

    @Override
    public void abortMultipartUpload(String bucketName, String objectName, String uploadId) throws Exception {
        this.minioClient.abortMultipartUpload(bucketName, objectName, uploadId);
    }

    @Override
    public String toString() {
        return "启用 FileStorage " + this.getChannelName()
                + " 默认分区:" + this.getDefaultBucketName()
                + " 访问根路径:" + this.getEndpoint()
                + " 服务器地址:" + this.getServerUrl();
    }
}
