package com.winter.cloud.common.filestorage.service.impl;

import com.winter.cloud.common.filestorage.support.lock.DistributionLockCli;
import com.winter.cloud.common.filestorage.support.constant.StorageConstants;
import com.winter.cloud.common.filestorage.support.exception.ExceptionUtils;
import com.winter.cloud.common.core.exception.ServiceException;
import com.winter.cloud.common.filestorage.support.cache.ProxyCache;
import com.winter.cloud.common.filestorage.support.cache.ProxyCacheManager;
import com.winter.cloud.common.core.utils.AutoMapUtils;
import com.winter.cloud.common.core.utils.StringUtils;
import com.winter.cloud.common.filestorage.FileInfo;
import com.winter.cloud.common.filestorage.clients.minio.MinioMultiPartUploadOptions;
import com.winter.cloud.common.filestorage.clients.minio.MinioObjectOptions;
import com.winter.cloud.common.filestorage.clients.minio.MinioStorageClient;
import com.winter.cloud.common.filestorage.clients.minio.PartUploadTag;
import com.winter.cloud.common.filestorage.dto.*;
import com.winter.cloud.common.filestorage.enums.FsBigFileUploadTaskStatusEnum;
import com.winter.cloud.common.filestorage.properties.WinterStorageProperties;
import com.winter.cloud.common.filestorage.service.BigFileUploadMinioService;
import com.winter.cloud.common.filestorage.service.FsBigFileUploadTaskService;
import com.winter.cloud.common.filestorage.support.web.DownloadFileInput;
import com.winter.cloud.common.filestorage.support.web.DownloadUtils;
import io.minio.ObjectStat;
import io.minio.messages.ListPartsResult;
import io.minio.messages.Part;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2023/12/13 14:44
 */
@Slf4j
public class BigFileUploadMinioServiceImpl implements BigFileUploadMinioService {

    /**
     * 缓存名称
     */
    private static final String CACHE_NAME = "big_file_Upload";

    /**
     * 锁前缀
     */
    private static final String LOCK_PREFIX = "big_file_Upload_lock";

    private final MinioStorageClient minioStorageClient;
    private final DistributionLockCli distributionLockCli;
    private final ProxyCacheManager proxyCacheManager;
    private final FsBigFileUploadTaskService fsBigFileUploadTaskService;
    private final WinterStorageProperties winterStorageProperties;

    /**
     * 上传部份大小(每次上传到文件服务器的块大小)
     */
    private final static long DEFAULT_PART_BYTES = 1024 * 1024 * 10;

    private static AtomicInteger uploadBigFileCount = new AtomicInteger(0);

    public BigFileUploadMinioServiceImpl(MinioStorageClient minioStorageClient, DistributionLockCli distributionLockCli,
                                         ProxyCacheManager proxyCacheManager, FsBigFileUploadTaskService fsBigFileUploadTaskService,
                                         WinterStorageProperties winterStorageProperties) {
        this.minioStorageClient = minioStorageClient;
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
        // 检查数据库，是否需要上传，如果不需要则可用采用复制。
        if (StringUtils.isNotEmpty(input.getFileMd5())) {
            FsBigFileUploadTaskInput dbBigFileUploadTask = fsBigFileUploadTaskService.queryByFileMd5(input.getFileMd5());
            if (Objects.nonNull(dbBigFileUploadTask)) {
                UploadApplyOutput result = AutoMapUtils.map(dbBigFileUploadTask, UploadApplyOutput.class);
                result.setExistSource(true);
                return result;
            }
        }
        long partBytes = Objects.isNull(input.getPartBytes()) ? DEFAULT_PART_BYTES : input.getPartBytes();
        MinioObjectOptions objectOptions = new MinioObjectOptions(input.getTotalBytes(), partBytes);
        objectOptions.setContentType(minioStorageClient.getFileContentType(input.getFileType()));
        MinioMultiPartUploadOptions uploadOptions = null;

        String bucketName = StringUtils.isNotEmpty(input.getBucketName()) ? input.getBucketName() : minioStorageClient.getDefaultBucketName();
        FileInfo fileInfo = new FileInfo(input.getFilePath(), true);
        try {
            uploadOptions = minioStorageClient.getMinioClient().createMultipartUploadOptions(bucketName, fileInfo.getFullPath(), objectOptions);
            UploadApplyOutput result = this.createUploadApplyOutput(input, uploadOptions, fileInfo);
            MultiPartUploadStatus status = this.createMultiPartUploadStatus(uploadOptions, result);
            // 保存到数据库,并设置id，并将状态设置 待上传，并设置过期时间，可采用扫描的形式删除时间过长而未完成的上传。
            FsBigFileUploadTaskInput bigFileUploadTask = fsBigFileUploadTaskService.add(AutoMapUtils.map(result, FsBigFileUploadTaskInput.class));
            status.setId(bigFileUploadTask.getId());
            this.saveUploadStatusCache(status);
            return result;
        } catch (Exception e) {
            if (Objects.nonNull(uploadOptions)) {
                this.abortMultipartUpload(uploadOptions);
            }
            throw ExceptionUtils.throwValidException(e.getMessage(), e);
        }
    }

    /**
     * 创建上传申请出参
     *
     * @param uploadApplyInput
     * @param uploadOptions    上传选项
     * @param fileInfo         文件信息
     * @return
     */
    private UploadApplyOutput createUploadApplyOutput(UploadApplyInput uploadApplyInput, MinioMultiPartUploadOptions uploadOptions, FileInfo fileInfo) {
        UploadApplyOutput result = AutoMapUtils.map(uploadApplyInput, UploadApplyOutput.class);
        result.setExistSource(false);
        result.setBucketName(uploadOptions.getBucketName());
        result.setFilePath(fileInfo.getFullPath());
        result.setAccessPath(minioStorageClient.getAccessUrl(uploadOptions.getBucketName(), fileInfo));
        result.setUploadId(uploadOptions.getUploadId());
        result.setPartBytes(uploadOptions.getPartSize());
        result.setPartCount(uploadOptions.getPartCount());
        result.setStatus(FsBigFileUploadTaskStatusEnum.NEW.getCode());
        return result;
    }

    /**
     * 创建部份上传状态
     *
     * @param uploadOptions 上传选项
     * @param result        结果
     * @return
     */
    protected MultiPartUploadStatus createMultiPartUploadStatus(MinioMultiPartUploadOptions uploadOptions, UploadApplyOutput result) {
        MultiPartUploadStatus status = new MultiPartUploadStatus();
        AutoMapUtils.mapForLoad(result, status);
        status.setOptions(uploadOptions);
        status.setTotalParts(new ArrayList<>(uploadOptions.getPartCount()));
        return status;
    }

    /**
     * 保存上传状态缓存
     *
     * @param status 状态
     */
    private void saveUploadStatusCache(MultiPartUploadStatus status) {
        ProxyCache cache = proxyCacheManager.getCache(CACHE_NAME);
        cache.put(status.getOptions().getUploadId(), status);
    }

    /**
     * 读取上传状态缓存
     *
     * @param uploadId 上传id
     * @return
     */
    private MultiPartUploadStatus readUploadStatusCache(String uploadId) {
        ProxyCache cache = proxyCacheManager.getCache(CACHE_NAME);
        return cache.get(uploadId, MultiPartUploadStatus.class);
    }

    /**
     * 清除上传状态缓存
     *
     * @param uploadId
     */
    private void clearUploadStatusCache(String uploadId) {
        proxyCacheManager.clearCacheKey(CACHE_NAME, uploadId);
    }

    /**
     * 验证状态
     *
     * @param uploadId 上传id
     * @return
     */
    protected MultiPartUploadStatus validationStatus(String uploadId) throws Exception {
        MultiPartUploadStatus status = this.readUploadStatusCache(uploadId);
        if (Objects.nonNull(status)) {
            return status;
        }
        //缓存不存在,从数据库获取
        FsBigFileUploadTaskInput bigFileUploadTask = fsBigFileUploadTaskService.queryByUploadId(uploadId);
        if (Objects.isNull(bigFileUploadTask)) {
            ExceptionUtils.throwValidException(String.format("不存在的上传id[%s]。", uploadId));
        }
        if (FsBigFileUploadTaskStatusEnum.COMPLETE_UPLOAD.getCode().equals(bigFileUploadTask.getStatus())) {
            ExceptionUtils.throwValidException(String.format("上传id[%s]已完成上传", uploadId));
        }
        status = AutoMapUtils.map(bigFileUploadTask, MultiPartUploadStatus.class);
        status.setOptions(new MinioMultiPartUploadOptions(bigFileUploadTask.getBucketName(),
                bigFileUploadTask.getFilePath(), uploadId, bigFileUploadTask.getTotalBytes(), bigFileUploadTask.getPartBytes(), new HashMap<>()));
        ListPartsResult listPartsResult = minioStorageClient.getMinioClient().listParts(bigFileUploadTask.getBucketName(), bigFileUploadTask.getFilePath(), null, null, uploadId);
        if (Objects.nonNull(listPartsResult) && !CollectionUtils.isEmpty(listPartsResult.partList())) {
            List<PartUploadTag> totalParts = new ArrayList<>(listPartsResult.partList().size());
            listPartsResult.partList().forEach(v -> totalParts.add(new PartUploadTag(v.partNumber(), v.etag())));
            status.setTotalParts(totalParts);
        }
        return status;
    }

    /**
     * 创建锁 Key
     *
     * @param uploadId 上传id
     * @return
     */
    private String createLockKey(String uploadId) {
        return String.join(StorageConstants.COLON, LOCK_PREFIX, uploadId);
    }

    @Override
    public UploadOutput multipartUpload(UploadInput input) {
        ExceptionUtils.checkNotNull(input, "input");
        input.valid();
        MultiPartUploadStatus status = null;
        // 是否加锁根据需求，如果不加锁则可能在取消上传或多现客户端不正确的上传或并发上传就会出错
        String lockKey = String.join(StorageConstants.COLON, LOCK_PREFIX, input.getUploadId(), String.valueOf(input.getPartNumber()));
        Integer bigFileUploadTaskStatus = FsBigFileUploadTaskStatusEnum.UPLOADING.getCode();
        uploadBigFileCount.incrementAndGet();
        try {
            if (uploadBigFileCount.get() > winterStorageProperties.getUploadBigFileCount()) {
                throw new ServiceException("超过最大同时上传处理数量,请稍后再试");
            }
            String lock = this.distributionLockCli.lock(lockKey);
            if (StringUtils.isEmpty(lock)) {
                throw new ServiceException("请勿重复提交");
            }
            status = this.validationStatus(input.getUploadId());
            //已上传过的分片,且不覆盖则直接返回
            if (status.getUploadedPartNumberList().contains(input.getPartNumber()) && !input.isOverride()) {
                return this.createUploadOutputResult(status, 0L);
            }
            long uploadedSize = status.getCompleteBytes();
            if (uploadedSize >= status.getOptions().getObjectSize()) {
                ExceptionUtils.throwValidException("已上传字节数超过文件总字节数。");
            }

            // 每次读取
            byte[] bytes = new byte[10240];
            int len;
            long tempTotalBytes = 0L;
            long currentBytes = 0L;
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream((int) status.getOptions().getPartSize());
            InputStream stream;
            if (input.isZipCompress()) {
                stream = this.upZipCompress(input.getInputStream());
            } else {
                stream = input.getInputStream();
            }
            while ((len = stream.read(bytes, 0, bytes.length)) > 0) {
                tempTotalBytes += len;
                uploadedSize += len;
                currentBytes += len;
                if (uploadedSize > status.getOptions().getObjectSize()) {
                    ExceptionUtils.throwValidException("已上传字节数超过文件总字节数。");
                }
                outputStream.write(bytes, 0, len);
                if (tempTotalBytes >= status.getOptions().getPartSize()) {
                    this.writeMultiPartUpload(status, outputStream, input.getPartNumber(), tempTotalBytes);
                    outputStream = new ByteArrayOutputStream((int) status.getOptions().getPartSize());
                    tempTotalBytes = 0L;
                }
            }
            if (tempTotalBytes > 0L) {
                this.writeMultiPartUpload(status, outputStream, input.getPartNumber(), tempTotalBytes);
            }

            status.setStatus(bigFileUploadTaskStatus);
            if (this.updateMultipartUpload(status)) {
                bigFileUploadTaskStatus = FsBigFileUploadTaskStatusEnum.COMPLETE_UPLOAD.getCode();
            }
            return this.createUploadOutputResult(status, currentBytes);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new ServiceException(e.getMessage());
        } finally {
            IOUtils.closeQuietly(input.getInputStream());
            // 更新数据库状态
            if (Objects.nonNull(status)) {
                fsBigFileUploadTaskService.updateCompleteBytesAndStatus(input.getUploadId(), status.getCompleteBytes(), bigFileUploadTaskStatus, status.getUploadedPartNumberList());
            }
            this.distributionLockCli.unlock(lockKey);
            uploadBigFileCount.decrementAndGet();
        }
    }

    /**
     * 写入多部份上传
     *
     * @param status        状态
     * @param outputStream  输出流
     * @param partNumber    部份编号
     * @param availableSize 可用大小
     * @throws Exception
     */
    private void writeMultiPartUpload(MultiPartUploadStatus status, ByteArrayOutputStream outputStream, int partNumber, long availableSize)
            throws Exception {
        PartUploadTag part = minioStorageClient.getMinioClient().multiPartUpload(status.getOptions(),
                outputStream.toByteArray(), partNumber, (int) availableSize);
        status.getTotalParts().add(part);
        IOUtils.closeQuietly(outputStream);
    }

    /**
     * 更新多部份上传
     *
     * @param status 状态
     * @return 返回 true 表示已完成，反之未完成
     * @throws Exception
     */
    private boolean updateMultipartUpload(MultiPartUploadStatus status)
            throws Exception {
        ListPartsResult listPartsResult = minioStorageClient.getMinioClient().listParts(status.getBucketName(), status.getFilePath(), null, null, status.getUploadId());
        List<Part> parts = listPartsResult.partList();
        status.setCompleteBytes(parts.stream().mapToLong(Part::partSize).sum());
        status.setTotalParts(new ArrayList<>(listPartsResult.partList().size()));
        parts.forEach(v -> status.getTotalParts().add(new PartUploadTag(v.partNumber(), v.etag())));
        status.setUploadedPartNumberList(parts.stream().map(Part::partNumber).collect(Collectors.toSet()));
        status.setUploadedPartCount(status.getUploadedPartNumberList().size());
        //上传完成
        if (status.getCompleteBytes() >= status.getTotalBytes()) {
            minioStorageClient.getMinioClient().completeMultipartUpload(status.getBucketName(), status.getFilePath(), status.getUploadId(), parts);
            fsBigFileUploadTaskService.updateCompleteBytesAndStatus(status.getId(), status.getTotalBytes(),
                    FsBigFileUploadTaskStatusEnum.COMPLETE_UPLOAD.getCode(), new HashSet<>(parts.stream().map(Part::partNumber).collect(Collectors.toList())));
            this.clearUploadStatusCache(status.getUploadId());
            return true;
        }
        this.saveUploadStatusCache(status);
        return false;
    }

    /**
     * 创建上传输出
     *
     * @param status       状态
     * @param currentBytes 当前大小
     * @return
     */
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
            // 更新数据库的申请表
            this.abortMultipartUpload(status.getOptions());
            this.clearUploadStatusCache(input.getUploadId());
            this.fsBigFileUploadTaskService.cancel(status.getId());
        } catch (Exception e) {
            throw new ServiceException(e.getMessage());
        } finally {
            this.distributionLockCli.unlock(lockKey);
        }
    }

    /**
     * 终止多部份上传
     *
     * @param options 选项
     * @return
     */
    private boolean abortMultipartUpload(MinioMultiPartUploadOptions options) {
        try {
            minioStorageClient.getMinioClient().abortMultipartUpload(options);
            return true;
        } catch (Exception err) {
            return false;
        }
    }

    @Override
    public UploadStatus queryMultipartStatus(UploadOptionsDto input) {
        ExceptionUtils.checkNotNull(input, "input");
        input.valid();
        // 是否加锁根据需求
        MultiPartUploadStatus status = this.readUploadStatusCache(input.getUploadId());
        if (Objects.isNull(status)) {
            FsBigFileUploadTaskInput bigFileUploadTask = fsBigFileUploadTaskService.queryByUploadId(input.getUploadId());
            if (Objects.isNull(bigFileUploadTask)) {
                throw new ServiceException(String.format("无效uploadId:%s", input.getUploadId()));
            }
            return AutoMapUtils.map(bigFileUploadTask, UploadStatus.class);
        }
        UploadStatus result = new UploadStatus();
        AutoMapUtils.mapForLoad(status, result);
        return result;
    }

    @Override
    public void download(String bucketName, String path, HttpServletRequest request, HttpServletResponse response) throws Exception {
        FileInfo fileInfo = new FileInfo(path, true);
        String defaultBucketName = StringUtils.isNotEmpty(bucketName) ? bucketName : minioStorageClient.getDefaultBucketName();
        ObjectStat stat = minioStorageClient.getMinioClient().statObject(defaultBucketName, fileInfo.getFullPath());
        if (stat == null) {
            throw new FileNotFoundException("文件不存在。");
        }
        DownloadFileInput input = new DownloadFileInput();
        input.setFileName(fileInfo.getName());
        input.setContentType(stat.contentType());
        input.setFileSize(stat.length());
        input.setFileStream(minioStorageClient.getMinioClient().getObject(defaultBucketName, fileInfo.getFullPath()));
        DownloadUtils.download(input, request, response, 9012);
    }

    @Override
    public void merge(String uploadId) throws Exception {
        FsBigFileUploadTaskInput bigFileUploadTask = fsBigFileUploadTaskService.queryByUploadId(uploadId);
        if (Objects.isNull(bigFileUploadTask)) {
            throw new ServiceException(String.format("不存在的uploadId[%s]", uploadId));
        }
        if (FsBigFileUploadTaskStatusEnum.COMPLETE_UPLOAD.getCode().equals(bigFileUploadTask.getStatus())) {
            throw new ServiceException(String.format("已完成上传uploadId[%s],无需再合并", uploadId));
        }
        ListPartsResult listPartsResult = minioStorageClient.getMinioClient().listParts(bigFileUploadTask.getBucketName(), bigFileUploadTask.getFilePath(), null, null, uploadId);
        List<Part> parts = listPartsResult.partList();
        if (parts.size() < bigFileUploadTask.getPartCount()) {
            throw new ServiceException(String.format("未上传uploadId[%s]完,不可合并", uploadId));
        }
        minioStorageClient.getMinioClient().completeMultipartUpload(bigFileUploadTask.getBucketName(), bigFileUploadTask.getFilePath(), uploadId, parts);
        fsBigFileUploadTaskService.updateCompleteBytesAndStatus(bigFileUploadTask.getId(), bigFileUploadTask.getTotalBytes(),
                FsBigFileUploadTaskStatusEnum.COMPLETE_UPLOAD.getCode(), new HashSet<>(parts.stream().map(Part::partNumber).collect(Collectors.toList())));
        this.clearUploadStatusCache(uploadId);
    }

    /**
     * 解压Zip流
     *
     * @param stream
     * @return
     */
    protected InputStream upZipCompress(InputStream stream) {
        return stream;
    }
}
