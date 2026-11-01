package com.winter.cloud.common.filestorage.clients.minio;

import io.minio.MinioClient;
import io.minio.ServerSideEncryption;
import io.minio.errors.*;
import io.minio.messages.ListPartsResult;
import io.minio.messages.Part;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 支持大文件
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2022/8/17 13:25
 */
public class MinioBigClient extends MinioClient {

    public MinioBigClient(String endpoint) throws InvalidEndpointException, InvalidPortException {
        super(endpoint);
    }

    public MinioBigClient(URL url) throws InvalidEndpointException, InvalidPortException {
        super(url);
    }

    public MinioBigClient(HttpUrl url) throws InvalidEndpointException, InvalidPortException {
        super(url);
    }

    public MinioBigClient(String endpoint, String accessKey, String secretKey) throws InvalidEndpointException, InvalidPortException {
        super(endpoint, accessKey, secretKey);
    }

    public MinioBigClient(String endpoint, String accessKey, String secretKey, String region) throws InvalidEndpointException, InvalidPortException {
        super(endpoint, accessKey, secretKey, region);
    }

    public MinioBigClient(URL url, String accessKey, String secretKey) throws InvalidEndpointException, InvalidPortException {
        super(url, accessKey, secretKey);
    }

    public MinioBigClient(HttpUrl url, String accessKey, String secretKey) throws InvalidEndpointException, InvalidPortException {
        super(url, accessKey, secretKey);
    }

    public MinioBigClient(String endpoint, int port, String accessKey, String secretKey) throws InvalidEndpointException, InvalidPortException {
        super(endpoint, port, accessKey, secretKey);
    }

    public MinioBigClient(String endpoint, String accessKey, String secretKey, boolean secure) throws InvalidEndpointException, InvalidPortException {
        super(endpoint, accessKey, secretKey, secure);
    }

    public MinioBigClient(String endpoint, int port, String accessKey, String secretKey, boolean secure) throws InvalidEndpointException, InvalidPortException {
        super(endpoint, port, accessKey, secretKey, secure);
    }

    public MinioBigClient(String endpoint, int port, String accessKey, String secretKey, String region, boolean secure) throws InvalidEndpointException, InvalidPortException {
        super(endpoint, port, accessKey, secretKey, region, secure);
    }

    public MinioBigClient(String endpoint, int port, String accessKey, String secretKey, String region, boolean secure, OkHttpClient httpClient) throws InvalidEndpointException, InvalidPortException {
        super(endpoint, port, accessKey, secretKey, region, secure, httpClient);
    }


    /**
     * 获取可用大小
     *
     * @param data             数据
     * @param expectedReadSize 预期读取大小
     * @return
     * @throws IOException
     * @throws InternalException
     */
    public long getAvailableSize(Object data, long expectedReadSize)
            throws IOException, InternalException {
        if (!(data instanceof BufferedInputStream)) {
            throw new InternalException(
                    "data must be BufferedInputStream. This should not happen.  "
                            + "Please report to https://github.com/minio/minio-java/issues/");
        }
        BufferedInputStream stream = (BufferedInputStream) data;
        stream.mark((int) expectedReadSize);
        byte[] buf = new byte[16384]; // 16KiB buffer for optimization
        long totalBytesRead = 0;
        while (totalBytesRead < expectedReadSize) {
            long bytesToRead = expectedReadSize - totalBytesRead;
            if (bytesToRead > buf.length) {
                bytesToRead = buf.length;
            }
            int bytesRead = stream.read(buf, 0, (int) bytesToRead);
            if (bytesRead < 0) {
                break; // reached EOF
            }
            totalBytesRead += bytesRead;
        }
        stream.reset();
        return totalBytesRead;
    }

    /**
     * 创建多个部份上传选项
     *
     * @param bucketName 桶
     * @param objectName 对象名称
     * @param options    选项
     * @return
     */
    public MinioMultiPartUploadOptions createMultipartUploadOptions(String bucketName, String objectName, MinioObjectOptions options)
            throws IOException,
            InvalidKeyException,
            NoSuchAlgorithmException,
            InsufficientDataException,
            InvalidResponseException,
            InternalException,
            XmlParserException,
            InvalidBucketNameException,
            ErrorResponseException,
            RegionConflictException {
        Map<String, String> headerMap = new HashMap<>();
        if (options.headers() != null) {
            headerMap.putAll(options.headers());
        }
        if (options.sse() != null) {
            headerMap.putAll(options.sse().headers());
        }
        headerMap.put("Content-Type", options.contentType());
        String uploadId = createMultipartUpload(bucketName, objectName, headerMap);
        Map<String, String> ssecHeaders = null;
        if (options.sse() != null && options.sse().type() == ServerSideEncryption.Type.SSE_C) {
            ssecHeaders = options.sse().headers();
        }
        if (!this.bucketExists(bucketName)) {
            this.makeBucket(bucketName);
        }
        return new MinioMultiPartUploadOptions(bucketName, objectName, uploadId, options.getObjectSize(), options.getPartSize(), ssecHeaders);
    }

    /**
     * 上传部份
     *
     * @param options       选项
     * @param stream        数据流
     * @param partNumber    部份编号，从1开始
     * @param availableSize 可用大小
     * @return
     */
    public PartUploadTag multiPartUpload(MinioMultiPartUploadOptions options, InputStream stream, int partNumber, int availableSize)
            throws IOException,
            InvalidKeyException,
            NoSuchAlgorithmException,
            InsufficientDataException,
            InvalidResponseException,
            InternalException,
            XmlParserException,
            InvalidBucketNameException,
            ErrorResponseException {
        if (!(stream instanceof BufferedInputStream)) {
            stream = new BufferedInputStream(stream);
        }
        String etag = this.uploadPart(
                options.getBucketName(),
                options.getObjectName(),
                stream,
                availableSize,
                options.getUploadId(),
                partNumber,
                options.getSsecHeaders());
        return new PartUploadTag(partNumber, etag);
    }

    /**
     * 上传部份
     *
     * @param options       选项
     * @param bytes         字节数
     * @param partNumber    部份编号，从1开始
     * @param availableSize 可用大小
     * @return
     */
    public PartUploadTag multiPartUpload(MinioMultiPartUploadOptions options, byte[] bytes, int partNumber, int availableSize)
            throws IOException,
            InvalidKeyException,
            NoSuchAlgorithmException,
            InsufficientDataException,
            InvalidResponseException,
            InternalException,
            XmlParserException,
            InvalidBucketNameException,
            ErrorResponseException {
        String etag = this.uploadPart(
                options.getBucketName(),
                options.getObjectName(),
                bytes,
                availableSize,
                options.getUploadId(),
                partNumber,
                options.getSsecHeaders());
        return new PartUploadTag(partNumber, etag);
    }

    /**
     * 完成多部份上传
     *
     * @param options    选项
     * @param totalParts 所有部份
     * @throws IOException
     * @throws InvalidKeyException
     * @throws NoSuchAlgorithmException
     * @throws InsufficientDataException
     * @throws InvalidResponseException
     * @throws InternalException
     * @throws XmlParserException
     * @throws InvalidBucketNameException
     * @throws ErrorResponseException
     */
    public void completeMultipartUpload(MinioMultiPartUploadOptions options, List<PartUploadTag> totalParts)
            throws IOException,
            InvalidKeyException,
            NoSuchAlgorithmException,
            InsufficientDataException,
            InvalidResponseException,
            InternalException,
            XmlParserException,
            InvalidBucketNameException,
            ErrorResponseException {
        Part[] parts = new Part[totalParts.size()];
        for (int i = 0; i < totalParts.size(); i++) {
            PartUploadTag tag = totalParts.get(i);
            if (tag != null) {
                parts[i] = tag.createPart();
            }
        }
        this.completeMultipartUpload(options, parts);
    }

    /**
     * 完成多部份上传
     *
     * @param options    选项
     * @param totalParts 所有部份
     * @throws IOException
     * @throws InvalidKeyException
     * @throws NoSuchAlgorithmException
     * @throws InsufficientDataException
     * @throws InvalidResponseException
     * @throws InternalException
     * @throws XmlParserException
     * @throws InvalidBucketNameException
     * @throws ErrorResponseException
     */
    public void completeMultipartUpload(MinioMultiPartUploadOptions options, Part[] totalParts)
            throws IOException,
            InvalidKeyException,
            NoSuchAlgorithmException,
            InsufficientDataException,
            InvalidResponseException,
            InternalException,
            XmlParserException,
            InvalidBucketNameException,
            ErrorResponseException {
        this.completeMultipartUpload(options.getBucketName(), options.getObjectName(), options.getUploadId(), totalParts);
    }

    /**
     * 终止多部份上传
     *
     * @param options 选项
     * @throws IOException
     * @throws InvalidKeyException
     * @throws NoSuchAlgorithmException
     * @throws InsufficientDataException
     * @throws InvalidResponseException
     * @throws InternalException
     * @throws XmlParserException
     * @throws InvalidBucketNameException
     * @throws ErrorResponseException
     */
    public void abortMultipartUpload(MinioMultiPartUploadOptions options)
            throws IOException, InvalidKeyException, NoSuchAlgorithmException, InsufficientDataException, InvalidResponseException, InternalException, XmlParserException, InvalidBucketNameException, ErrorResponseException {
        this.abortMultipartUpload(options.getBucketName(), options.getObjectName(), options.getUploadId());
    }

    /**
     * 终止多部份上传
     *
     * @param bucketName 桶
     * @param objectName 对象名称
     * @param uploadId   上传id
     * @throws InvalidBucketNameException
     * @throws IllegalArgumentException
     * @throws NoSuchAlgorithmException
     * @throws InsufficientDataException
     * @throws IOException
     * @throws InvalidKeyException
     * @throws XmlParserException
     * @throws ErrorResponseException
     * @throws InternalException
     * @throws InvalidResponseException
     */
    @Override
    public void abortMultipartUpload(String bucketName, String objectName, String uploadId) throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException, InsufficientDataException, IOException, InvalidKeyException, XmlParserException, ErrorResponseException, InternalException, InvalidResponseException {
        super.abortMultipartUpload(bucketName, objectName, uploadId);
    }

    @Override
    public ListPartsResult listParts(String bucketName, String objectName, Integer maxParts, Integer partNumberMarker, String uploadId) throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException, InsufficientDataException, IOException, InvalidKeyException, XmlParserException, ErrorResponseException, InternalException, InvalidResponseException {
        return super.listParts(bucketName, objectName, maxParts, partNumberMarker, uploadId);
    }

    public void completeMultipartUpload(String bucketName, String objectName, String uploadId, List<Part> totalParts) throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException, InsufficientDataException, IOException, InvalidKeyException, XmlParserException, ErrorResponseException, InternalException, InvalidResponseException {
        Part[] parts = new Part[totalParts.size()];
        for (int i = 0; i < totalParts.size(); i++) {
            Part tag = totalParts.get(i);
            if (tag != null) {
                parts[i] = tag;
            }
        }
        super.completeMultipartUpload(bucketName, objectName, uploadId, parts);
    }
}
