package com.winter.cloud.common.filestorage.service.impl;

import com.winter.cloud.common.core.exception.ServiceException;
import com.winter.cloud.common.core.utils.AutoMapUtils;
import com.winter.cloud.common.core.utils.StringUtils;
import com.winter.cloud.common.filestorage.AbstractStorageClient;
import com.winter.cloud.common.filestorage.FileInfo;
import com.winter.cloud.common.filestorage.FileStorageObject;
import com.winter.cloud.common.filestorage.StorageClient;
import com.winter.cloud.common.filestorage.StorageClientContext;
import com.winter.cloud.common.filestorage.dto.*;
import com.winter.cloud.common.filestorage.enums.FsBigFileUploadTaskStatusEnum;
import com.winter.cloud.common.filestorage.multipart.MultipartListedPart;
import com.winter.cloud.common.filestorage.multipart.MultipartPartETag;
import com.winter.cloud.common.filestorage.multipart.MultipartUploadConstants;
import com.winter.cloud.common.filestorage.multipart.MultipartUploadOptions;
import com.winter.cloud.common.filestorage.multipart.MultipartUploadSupport;
import com.winter.cloud.common.filestorage.properties.WinterStorageProperties;
import com.winter.cloud.common.filestorage.service.BigFileUploadService;
import com.winter.cloud.common.filestorage.service.FsBigFileUploadTaskService;
import com.winter.cloud.common.filestorage.support.cache.ProxyCache;
import com.winter.cloud.common.filestorage.support.cache.ProxyCacheManager;
import com.winter.cloud.common.filestorage.support.constant.StorageConstants;
import com.winter.cloud.common.filestorage.support.exception.ExceptionUtils;
import com.winter.cloud.common.filestorage.support.lock.DistributionLockCli;
import com.winter.cloud.common.filestorage.support.web.DownloadFileInput;
import com.winter.cloud.common.filestorage.support.web.DownloadUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 通用大文件分片上传服务：按 StorageClient 通道分发到 MinIO / AWS / OSS / OBS / COS。
 */
@Slf4j
public class BigFileUploadServiceImpl implements BigFileUploadService {

    private static final String CACHE_NAME = "big_file_Upload";
    private static final String LOCK_PREFIX = "big_file_Upload_lock";

    private final StorageClientContext storageClientContext;
    private final DistributionLockCli distributionLockCli;
    private final ProxyCacheManager proxyCacheManager;
    private final FsBigFileUploadTaskService fsBigFileUploadTaskService;
    private final WinterStorageProperties winterStorageProperties;
    private static final AtomicInteger UPLOAD_BIG_FILE_COUNT = new AtomicInteger(0);

    public BigFileUploadServiceImpl(StorageClientContext storageClientContext,
                                    DistributionLockCli distributionLockCli,
                                    ProxyCacheManager proxyCacheManager,
                                    FsBigFileUploadTaskService fsBigFileUploadTaskService,
                                    WinterStorageProperties winterStorageProperties) {
        this.storageClientContext = storageClientContext;
        this.distributionLockCli = distributionLockCli;
        this.proxyCacheManager = proxyCacheManager;
        this.fsBigFileUploadTaskService = fsBigFileUploadTaskService;
        this.winterStorageProperties = winterStorageProperties;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UploadApplyOutput multipartUploadApply(UploadApplyInput input) {
        ExceptionUtils.checkNotNull(input, "input");
        input.valid();
        if (StringUtils.isNotEmpty(input.getFileMd5())) {
            FsBigFileUploadTaskInput dbTask = fsBigFileUploadTaskService.queryByFileMd5(input.getFileMd5());
            if (Objects.nonNull(dbTask)) {
                UploadApplyOutput result = AutoMapUtils.map(dbTask, UploadApplyOutput.class);
                result.setExistSource(true);
                return result;
            }
        }

        StorageClient storageClient = this.resolveStorageClient(input.getChannelId());
        MultipartUploadSupport multipartClient = this.asMultipart(storageClient);
        AbstractStorageClient<?> abstractClient = this.asAbstract(storageClient);

        long partBytes = Objects.isNull(input.getPartBytes())
                ? MultipartUploadConstants.DEFAULT_PART_BYTES : input.getPartBytes();
        String bucketName = StringUtils.isNotEmpty(input.getBucketName())
                ? input.getBucketName() : storageClient.getDefaultBucketName();
        FileInfo fileInfo = new FileInfo(input.getFilePath(), true);
        MultipartUploadOptions uploadOptions = null;
        try {
            String contentType = abstractClient.getFileContentType(input.getFileType());
            uploadOptions = multipartClient.initiateMultipartUpload(
                    bucketName, fileInfo.getFullPath(), input.getTotalBytes(), partBytes, contentType);
            UploadApplyOutput result = this.createUploadApplyOutput(input, storageClient, uploadOptions, fileInfo);
            MultiPartUploadStatus status = this.createMultiPartUploadStatus(uploadOptions, result);
            FsBigFileUploadTaskInput task = fsBigFileUploadTaskService.add(
                    AutoMapUtils.map(result, FsBigFileUploadTaskInput.class));
            status.setId(task.getId());
            this.saveUploadStatusCache(status);
            return result;
        } catch (Exception e) {
            if (Objects.nonNull(uploadOptions)) {
                this.abortMultipartUpload(multipartClient, uploadOptions);
            }
            throw ExceptionUtils.throwValidException(e.getMessage(), e);
        }
    }

    private UploadApplyOutput createUploadApplyOutput(UploadApplyInput uploadApplyInput,
                                                      StorageClient storageClient,
                                                      MultipartUploadOptions uploadOptions,
                                                      FileInfo fileInfo) {
        UploadApplyOutput result = AutoMapUtils.map(uploadApplyInput, UploadApplyOutput.class);
        result.setExistSource(false);
        result.setChannelId(storageClient.getChannelId());
        result.setBucketName(uploadOptions.getBucketName());
        result.setFilePath(fileInfo.getFullPath());
        result.setAccessPath(storageClient.getAccessUrl(uploadOptions.getBucketName(), fileInfo.getFullPath()));
        result.setUploadId(uploadOptions.getUploadId());
        result.setPartBytes(uploadOptions.getPartSize());
        result.setPartCount(uploadOptions.getPartCount());
        result.setStatus(FsBigFileUploadTaskStatusEnum.NEW.getCode());
        return result;
    }

    private MultiPartUploadStatus createMultiPartUploadStatus(MultipartUploadOptions uploadOptions,
                                                              UploadApplyOutput result) {
        MultiPartUploadStatus status = new MultiPartUploadStatus();
        AutoMapUtils.mapForLoad(result, status);
        status.setChannelId(uploadOptions.getChannelId());
        status.setOptions(uploadOptions);
        status.setTotalParts(new ArrayList<>(uploadOptions.getPartCount()));
        return status;
    }

    private void saveUploadStatusCache(MultiPartUploadStatus status) {
        ProxyCache cache = proxyCacheManager.getCache(CACHE_NAME);
        cache.put(status.getOptions().getUploadId(), status);
    }

    private MultiPartUploadStatus readUploadStatusCache(String uploadId) {
        ProxyCache cache = proxyCacheManager.getCache(CACHE_NAME);
        return cache.get(uploadId, MultiPartUploadStatus.class);
    }

    private void clearUploadStatusCache(String uploadId) {
        proxyCacheManager.clearCacheKey(CACHE_NAME, uploadId);
    }

    protected MultiPartUploadStatus validationStatus(String uploadId) throws Exception {
        MultiPartUploadStatus status = this.readUploadStatusCache(uploadId);
        if (Objects.nonNull(status)) {
            return status;
        }
        FsBigFileUploadTaskInput task = fsBigFileUploadTaskService.queryByUploadId(uploadId);
        if (Objects.isNull(task)) {
            ExceptionUtils.throwValidException(String.format("不存在的上传id[%s]。", uploadId));
        }
        if (FsBigFileUploadTaskStatusEnum.COMPLETE_UPLOAD.getCode().equals(task.getStatus())) {
            ExceptionUtils.throwValidException(String.format("上传id[%s]已完成上传", uploadId));
        }
        MultipartUploadSupport multipartClient = this.asMultipart(this.resolveStorageClient(task.getChannelId()));
        status = AutoMapUtils.map(task, MultiPartUploadStatus.class);
        status.setChannelId(task.getChannelId());
        status.setOptions(new MultipartUploadOptions(
                task.getChannelId(), task.getBucketName(), task.getFilePath(), uploadId,
                task.getTotalBytes(), task.getPartBytes(), null, new HashMap<>()));
        List<MultipartListedPart> listedParts = multipartClient.listMultipartParts(
                task.getBucketName(), task.getFilePath(), uploadId);
        if (!CollectionUtils.isEmpty(listedParts)) {
            List<MultipartPartETag> totalParts = new ArrayList<>(listedParts.size());
            listedParts.forEach(part -> totalParts.add(new MultipartPartETag(part.getPartNumber(), part.getEtag())));
            status.setTotalParts(totalParts);
        }
        return status;
    }

    private String createLockKey(String uploadId) {
        return String.join(StorageConstants.COLON, LOCK_PREFIX, uploadId);
    }

    @Override
    public UploadOutput multipartUpload(UploadInput input) {
        ExceptionUtils.checkNotNull(input, "input");
        input.valid();
        MultiPartUploadStatus status = null;
        String lockKey = String.join(StorageConstants.COLON, LOCK_PREFIX, input.getUploadId(),
                String.valueOf(input.getPartNumber()));
        Integer taskStatus = FsBigFileUploadTaskStatusEnum.UPLOADING.getCode();
        UPLOAD_BIG_FILE_COUNT.incrementAndGet();
        try {
            if (UPLOAD_BIG_FILE_COUNT.get() > winterStorageProperties.getUploadBigFileCount()) {
                throw new ServiceException("超过最大同时上传处理数量,请稍后再试");
            }
            String lock = this.distributionLockCli.lock(lockKey);
            if (StringUtils.isEmpty(lock)) {
                throw new ServiceException("请勿重复提交");
            }
            status = this.validationStatus(input.getUploadId());
            if (status.getUploadedPartNumberList().contains(input.getPartNumber()) && !input.isOverride()) {
                return this.createUploadOutputResult(status, 0L);
            }
            long uploadedSize = status.getCompleteBytes();
            if (uploadedSize >= status.getOptions().getObjectSize()) {
                ExceptionUtils.throwValidException("已上传字节数超过文件总字节数。");
            }

            MultipartUploadSupport multipartClient = this.asMultipart(
                    this.resolveStorageClient(status.getChannelId()));
            byte[] bytes = new byte[10240];
            int len;
            long tempTotalBytes = 0L;
            long currentBytes = 0L;
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream((int) status.getOptions().getPartSize());
            InputStream stream = input.isZipCompress()
                    ? this.upZipCompress(input.getInputStream()) : input.getInputStream();
            while ((len = stream.read(bytes, 0, bytes.length)) > 0) {
                tempTotalBytes += len;
                uploadedSize += len;
                currentBytes += len;
                if (uploadedSize > status.getOptions().getObjectSize()) {
                    ExceptionUtils.throwValidException("已上传字节数超过文件总字节数。");
                }
                outputStream.write(bytes, 0, len);
                if (tempTotalBytes >= status.getOptions().getPartSize()) {
                    this.writeMultiPartUpload(multipartClient, status, outputStream, input.getPartNumber(), tempTotalBytes);
                    outputStream = new ByteArrayOutputStream((int) status.getOptions().getPartSize());
                    tempTotalBytes = 0L;
                }
            }
            if (tempTotalBytes > 0L) {
                this.writeMultiPartUpload(multipartClient, status, outputStream, input.getPartNumber(), tempTotalBytes);
            }

            status.setStatus(taskStatus);
            if (this.updateMultipartUpload(multipartClient, status)) {
                taskStatus = FsBigFileUploadTaskStatusEnum.COMPLETE_UPLOAD.getCode();
            }
            return this.createUploadOutputResult(status, currentBytes);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new ServiceException(e.getMessage());
        } finally {
            IOUtils.closeQuietly(input.getInputStream());
            if (Objects.nonNull(status)) {
                fsBigFileUploadTaskService.updateCompleteBytesAndStatus(
                        input.getUploadId(), status.getCompleteBytes(), taskStatus, status.getUploadedPartNumberList());
            }
            this.distributionLockCli.unlock(lockKey);
            UPLOAD_BIG_FILE_COUNT.decrementAndGet();
        }
    }

    private void writeMultiPartUpload(MultipartUploadSupport multipartClient,
                                      MultiPartUploadStatus status,
                                      ByteArrayOutputStream outputStream,
                                      int partNumber,
                                      long availableSize) throws Exception {
        MultipartPartETag part = multipartClient.uploadPart(
                status.getOptions(), outputStream.toByteArray(), partNumber, (int) availableSize);
        status.getTotalParts().add(part);
        IOUtils.closeQuietly(outputStream);
    }

    private boolean updateMultipartUpload(MultipartUploadSupport multipartClient,
                                          MultiPartUploadStatus status) throws Exception {
        List<MultipartListedPart> listedParts = multipartClient.listMultipartParts(
                status.getBucketName(), status.getFilePath(), status.getUploadId());
        status.setCompleteBytes(listedParts.stream().mapToLong(MultipartListedPart::getSize).sum());
        List<MultipartPartETag> totalParts = listedParts.stream()
                .map(part -> new MultipartPartETag(part.getPartNumber(), part.getEtag()))
                .collect(Collectors.toList());
        status.setTotalParts(totalParts);
        status.setUploadedPartNumberList(listedParts.stream()
                .map(MultipartListedPart::getPartNumber).collect(Collectors.toSet()));
        status.setUploadedPartCount(status.getUploadedPartNumberList().size());
        if (status.getCompleteBytes() >= status.getTotalBytes()) {
            multipartClient.completeMultipartUpload(
                    status.getBucketName(), status.getFilePath(), status.getUploadId(), totalParts);
            fsBigFileUploadTaskService.updateCompleteBytesAndStatus(
                    status.getId(), status.getTotalBytes(),
                    FsBigFileUploadTaskStatusEnum.COMPLETE_UPLOAD.getCode(),
                    new HashSet<>(listedParts.stream().map(MultipartListedPart::getPartNumber).collect(Collectors.toList())));
            this.clearUploadStatusCache(status.getUploadId());
            return true;
        }
        this.saveUploadStatusCache(status);
        return false;
    }

    private UploadOutput createUploadOutputResult(MultiPartUploadStatus status, long currentBytes) {
        UploadOutput result = new UploadOutput();
        result.setUploadId(status.getUploadId());
        result.setFilePath(status.getFilePath());
        result.setAccessPath(status.getAccessPath());
        result.setTotalBytes(status.getOptions().getObjectSize());
        result.setCompleteBytes(status.getCompleteBytes());
        result.setCurrentBytes(currentBytes);
        result.setUploadedPartNumberList(status.getUploadedPartNumberList());
        return result;
    }

    @Override
    public void deleteMultipartUpload(UploadOptionsDto input) {
        ExceptionUtils.checkNotNull(input, "input");
        input.valid();
        String lockKey = this.createLockKey(input.getUploadId());
        try {
            String lock = this.distributionLockCli.lock(lockKey);
            if (StringUtils.isEmpty(lock)) {
                throw new ServiceException("请勿重复提交");
            }
            MultiPartUploadStatus status = this.validationStatus(input.getUploadId());
            MultipartUploadSupport multipartClient = this.asMultipart(
                    this.resolveStorageClient(status.getChannelId()));
            this.abortMultipartUpload(multipartClient, status.getOptions());
            this.clearUploadStatusCache(input.getUploadId());
            this.fsBigFileUploadTaskService.cancel(status.getId());
        } catch (Exception e) {
            throw new ServiceException(e.getMessage());
        } finally {
            this.distributionLockCli.unlock(lockKey);
        }
    }

    private boolean abortMultipartUpload(MultipartUploadSupport multipartClient, MultipartUploadOptions options) {
        try {
            multipartClient.abortMultipartUpload(options.getBucketName(), options.getObjectName(), options.getUploadId());
            return true;
        } catch (Exception err) {
            return false;
        }
    }

    @Override
    public UploadStatus queryMultipartStatus(UploadOptionsDto input) {
        ExceptionUtils.checkNotNull(input, "input");
        input.valid();
        MultiPartUploadStatus status = this.readUploadStatusCache(input.getUploadId());
        if (Objects.isNull(status)) {
            FsBigFileUploadTaskInput task = fsBigFileUploadTaskService.queryByUploadId(input.getUploadId());
            if (Objects.isNull(task)) {
                throw new ServiceException(String.format("无效uploadId:%s", input.getUploadId()));
            }
            return AutoMapUtils.map(task, UploadStatus.class);
        }
        UploadStatus result = new UploadStatus();
        AutoMapUtils.mapForLoad(status, result);
        return result;
    }

    @Override
    public void download(String channelId, String bucketName, String path,
                         HttpServletRequest request, HttpServletResponse response) throws Exception {
        StorageClient storageClient = this.resolveStorageClient(channelId);
        FileInfo fileInfo = new FileInfo(path, true);
        String defaultBucketName = StringUtils.isNotEmpty(bucketName)
                ? bucketName : storageClient.getDefaultBucketName();
        if (!storageClient.existFile(defaultBucketName, fileInfo.getFullPath())) {
            throw new FileNotFoundException("文件不存在。");
        }
        FileStorageObject storageObject = storageClient.getFile(defaultBucketName, fileInfo.getFullPath());
        if (storageObject == null || storageObject.getInputStream() == null) {
            throw new FileNotFoundException("文件不存在。");
        }
        DownloadFileInput input = new DownloadFileInput();
        input.setFileName(fileInfo.getName());
        input.setContentType(this.asAbstract(storageClient).getFileContentType(fileInfo.getExtensionName()));
        input.setFileSize(storageObject.getFileInfo() == null ? 0L : storageObject.getFileInfo().getLength());
        input.setFileStream(storageObject.getInputStream());
        DownloadUtils.download(input, request, response, 9012);
    }

    @Override
    public void merge(String uploadId) throws Exception {
        FsBigFileUploadTaskInput task = fsBigFileUploadTaskService.queryByUploadId(uploadId);
        if (Objects.isNull(task)) {
            throw new ServiceException(String.format("不存在的uploadId[%s]", uploadId));
        }
        if (FsBigFileUploadTaskStatusEnum.COMPLETE_UPLOAD.getCode().equals(task.getStatus())) {
            throw new ServiceException(String.format("已完成上传uploadId[%s],无需再合并", uploadId));
        }
        MultipartUploadSupport multipartClient = this.asMultipart(this.resolveStorageClient(task.getChannelId()));
        List<MultipartListedPart> listedParts = multipartClient.listMultipartParts(
                task.getBucketName(), task.getFilePath(), uploadId);
        if (listedParts.size() < task.getPartCount()) {
            throw new ServiceException(String.format("未上传uploadId[%s]完,不可合并", uploadId));
        }
        List<MultipartPartETag> parts = listedParts.stream()
                .map(part -> new MultipartPartETag(part.getPartNumber(), part.getEtag()))
                .collect(Collectors.toList());
        multipartClient.completeMultipartUpload(task.getBucketName(), task.getFilePath(), uploadId, parts);
        fsBigFileUploadTaskService.updateCompleteBytesAndStatus(
                task.getId(), task.getTotalBytes(),
                FsBigFileUploadTaskStatusEnum.COMPLETE_UPLOAD.getCode(),
                new HashSet<>(listedParts.stream().map(MultipartListedPart::getPartNumber).collect(Collectors.toList())));
        this.clearUploadStatusCache(uploadId);
    }

    /**
     * 解析通道：入参 channelId > 配置 multipart-channel（与普通上传一样由用户配置选择）。
     */
    protected StorageClient resolveStorageClient(String channelId) {
        String targetChannelId = StringUtils.isNotEmpty(channelId)
                ? channelId : winterStorageProperties.getMultipartChannel();
        if (StringUtils.isEmpty(targetChannelId)) {
            throw new ServiceException("未指定存储通道，请传入 channelId 或配置 winter.storage.client.multipart-channel");
        }
        StorageClient client = storageClientContext.getChannel(targetChannelId);
        if (client == null) {
            throw new ServiceException(String.format("未找到存储通道[%s]，请检查对应客户端是否已启用", targetChannelId));
        }
        return client;
    }

    private MultipartUploadSupport asMultipart(StorageClient client) {
        if (!(client instanceof MultipartUploadSupport)) {
            throw new ServiceException(String.format("存储通道[%s]不支持分片上传", client.getChannelId()));
        }
        return (MultipartUploadSupport) client;
    }

    private AbstractStorageClient<?> asAbstract(StorageClient client) {
        if (!(client instanceof AbstractStorageClient)) {
            throw new ServiceException(String.format("存储通道[%s]类型不支持", client.getChannelId()));
        }
        return (AbstractStorageClient<?>) client;
    }

    protected InputStream upZipCompress(InputStream stream) {
        return stream;
    }
}
