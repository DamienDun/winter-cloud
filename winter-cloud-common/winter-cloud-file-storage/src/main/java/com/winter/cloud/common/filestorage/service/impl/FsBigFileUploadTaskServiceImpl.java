package com.winter.cloud.common.filestorage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.winter.cloud.common.core.utils.AutoMapUtils;
import com.winter.cloud.common.core.utils.StringUtils;
import com.winter.cloud.common.filestorage.StorageClient;
import com.winter.cloud.common.filestorage.StorageClientContext;
import com.winter.cloud.common.filestorage.domain.FsBigFileUploadTask;
import com.winter.cloud.common.filestorage.dto.FsBigFileUploadTaskInput;
import com.winter.cloud.common.filestorage.dto.UploadOptionsDto;
import com.winter.cloud.common.filestorage.enums.FsBigFileUploadTaskStatusEnum;
import com.winter.cloud.common.filestorage.mapper.FsBigFileUploadTaskMapper;
import com.winter.cloud.common.filestorage.service.BigFileUploadService;
import com.winter.cloud.common.filestorage.service.FsBigFileUploadTaskService;
import com.winter.cloud.common.filestorage.support.constant.StorageConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 大文件上传任务服务实现
 *
 * @author Damien
 */
public class FsBigFileUploadTaskServiceImpl implements FsBigFileUploadTaskService {

    private static final Logger log = LoggerFactory.getLogger(FsBigFileUploadTaskServiceImpl.class);

    private final FsBigFileUploadTaskMapper fsBigFileUploadTaskMapper;
    /**
     * 用 ObjectProvider 延迟取 BigFileUploadService，避免与其构造期互相依赖形成环
     */
    private final ObjectProvider<BigFileUploadService> bigFileUploadServiceProvider;
    private final ObjectProvider<StorageClientContext> storageClientContextProvider;

    public FsBigFileUploadTaskServiceImpl(FsBigFileUploadTaskMapper fsBigFileUploadTaskMapper,
                                          ObjectProvider<BigFileUploadService> bigFileUploadServiceProvider,
                                          ObjectProvider<StorageClientContext> storageClientContextProvider) {
        this.fsBigFileUploadTaskMapper = fsBigFileUploadTaskMapper;
        this.bigFileUploadServiceProvider = bigFileUploadServiceProvider;
        this.storageClientContextProvider = storageClientContextProvider;
    }

    @Override
    public FsBigFileUploadTaskInput add(FsBigFileUploadTaskInput input) {
        FsBigFileUploadTask entity = AutoMapUtils.map(input, FsBigFileUploadTask.class);
        Date now = new Date();
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
        fsBigFileUploadTaskMapper.insert(entity);
        input.setId(entity.getId());
        return input;
    }

    @Override
    public void updateCompleteBytesAndStatus(Long id, Long completeBytes, Integer status, Set<Integer> uploadedPartNumberList) {
        FsBigFileUploadTask task = fsBigFileUploadTaskMapper.selectById(id);
        if (Objects.isNull(task)) {
            return;
        }
        fillProgress(task, completeBytes, status, uploadedPartNumberList);
        fsBigFileUploadTaskMapper.updateById(task);
    }

    @Override
    public void updateCompleteBytesAndStatus(String uploadId, Long completeBytes, Integer status, Set<Integer> uploadedPartNumberList) {
        LambdaQueryWrapper<FsBigFileUploadTask> wrapper = Wrappers.lambdaQuery(FsBigFileUploadTask.class);
        wrapper.eq(FsBigFileUploadTask::getUploadId, uploadId);
        FsBigFileUploadTask task = fsBigFileUploadTaskMapper.selectForFirst(wrapper);
        if (Objects.isNull(task)) {
            return;
        }
        fillProgress(task, completeBytes, status, uploadedPartNumberList);
        fsBigFileUploadTaskMapper.updateById(task);
    }

    @Override
    public FsBigFileUploadTaskInput queryByFileMd5(String fileMd5) {
        LambdaQueryWrapper<FsBigFileUploadTask> wrapper = Wrappers.lambdaQuery(FsBigFileUploadTask.class);
        wrapper.eq(FsBigFileUploadTask::getFileMd5, fileMd5);
        return AutoMapUtils.map(fsBigFileUploadTaskMapper.selectForFirst(wrapper), FsBigFileUploadTaskInput.class);
    }

    @Override
    public FsBigFileUploadTaskInput queryByUploadId(String uploadId) {
        LambdaQueryWrapper<FsBigFileUploadTask> wrapper = Wrappers.lambdaQuery(FsBigFileUploadTask.class);
        wrapper.eq(FsBigFileUploadTask::getUploadId, uploadId);
        return AutoMapUtils.map(fsBigFileUploadTaskMapper.selectForFirst(wrapper), FsBigFileUploadTaskInput.class);
    }

    @Override
    public void cancel(Long id) {
        FsBigFileUploadTask task = fsBigFileUploadTaskMapper.selectById(id);
        if (Objects.isNull(task)) {
            return;
        }
        task.setStatus(FsBigFileUploadTaskStatusEnum.CANCEL.getCode());
        task.setUpdateTime(new Date());
        fsBigFileUploadTaskMapper.updateById(task);
        fsBigFileUploadTaskMapper.deleteById(id);
    }

    @Override
    public List<FsBigFileUploadTask> selectTaskList(FsBigFileUploadTask query) {
        LambdaQueryWrapper<FsBigFileUploadTask> wrapper = Wrappers.lambdaQuery(FsBigFileUploadTask.class);
        if (query != null) {
            wrapper.eq(StringUtils.isNotEmpty(query.getUploadId()), FsBigFileUploadTask::getUploadId, query.getUploadId())
                .eq(StringUtils.isNotEmpty(query.getFileMd5()), FsBigFileUploadTask::getFileMd5, query.getFileMd5())
                .eq(StringUtils.isNotEmpty(query.getChannelId()), FsBigFileUploadTask::getChannelId, query.getChannelId())
                .eq(StringUtils.isNotEmpty(query.getFileType()), FsBigFileUploadTask::getFileType, query.getFileType())
                .eq(StringUtils.isNotEmpty(query.getBucketName()), FsBigFileUploadTask::getBucketName, query.getBucketName())
                .eq(Objects.nonNull(query.getStatus()), FsBigFileUploadTask::getStatus, query.getStatus())
                .like(StringUtils.isNotEmpty(query.getFilePath()), FsBigFileUploadTask::getFilePath, query.getFilePath())
                .like(StringUtils.isNotEmpty(query.getAccessPath()), FsBigFileUploadTask::getAccessPath, query.getAccessPath());
        }
        wrapper.orderByDesc(FsBigFileUploadTask::getCreateTime);
        return fsBigFileUploadTaskMapper.selectList(wrapper);
    }

    @Override
    public FsBigFileUploadTask selectById(Long id) {
        if (id == null) {
            return null;
        }
        return fsBigFileUploadTaskMapper.selectById(id);
    }

    @Override
    public int deleteByIds(Long[] ids) {
        if (ids == null || ids.length == 0) {
            return 0;
        }
        int rows = 0;
        for (Long id : ids) {
            if (deleteOne(id)) {
                rows++;
            }
        }
        return rows;
    }

    private boolean deleteOne(Long id) {
        FsBigFileUploadTask task = fsBigFileUploadTaskMapper.selectById(id);
        if (task == null) {
            log.warn("big-file task not found, id={}", id);
            return false;
        }
        boolean completed = FsBigFileUploadTaskStatusEnum.COMPLETE_UPLOAD.getCode().equals(task.getStatus());
        if (completed) {
            // 已完成：先删对象再删任务行
            tryDeleteObject(task);
        } else if (StringUtils.isNotEmpty(task.getUploadId())) {
            // 未完成：走分片中止（含清缓存、cancel 删行）
            try {
                BigFileUploadService uploadService = bigFileUploadServiceProvider.getIfAvailable();
                if (uploadService != null) {
                    UploadOptionsDto options = new UploadOptionsDto();
                    options.setUploadId(task.getUploadId());
                    uploadService.deleteMultipartUpload(options);
                    if (fsBigFileUploadTaskMapper.selectById(id) == null) {
                        return true;
                    }
                }
            } catch (Exception e) {
                log.warn("abort multipart failed, uploadId={}, fallback delete row: {}",
                    task.getUploadId(), e.getMessage());
            }
        }
        return fsBigFileUploadTaskMapper.deleteById(id) > 0 || fsBigFileUploadTaskMapper.selectById(id) == null;
    }

    private void tryDeleteObject(FsBigFileUploadTask task) {
        if (StringUtils.isEmpty(task.getFilePath())) {
            return;
        }
        StorageClientContext context = storageClientContextProvider.getIfAvailable();
        if (context == null) {
            log.warn("StorageClientContext unavailable, skip object delete, path={}", task.getFilePath());
            return;
        }
        try {
            StorageClient client = resolveClient(context, task.getChannelId());
            String bucket = StringUtils.isNotEmpty(task.getBucketName())
                ? task.getBucketName()
                : client.getDefaultBucketName();
            client.deleteFile(bucket, task.getFilePath());
        } catch (Exception e) {
            // 对象可能已不存在，仍允许删库表行
            log.warn("delete big-file object failed, path={}: {}", task.getFilePath(), e.getMessage());
        }
    }

    private StorageClient resolveClient(StorageClientContext context, String channelId) {
        if (StringUtils.isNotEmpty(channelId)) {
            StorageClient client = context.getChannel(channelId);
            if (client != null) {
                return client;
            }
        }
        Collection<StorageClient> channels = context.channels();
        if (channels == null || channels.isEmpty()) {
            throw new IllegalStateException("no storage client enabled");
        }
        return channels.iterator().next();
    }

    private void fillProgress(FsBigFileUploadTask task, Long completeBytes, Integer status, Set<Integer> uploadedPartNumberList) {
        task.setCompleteBytes(completeBytes);
        task.setStatus(status);
        task.setUpdateTime(new Date());
        if (!CollectionUtils.isEmpty(uploadedPartNumberList)) {
            task.setUploadedPartNumbers(uploadedPartNumberList.stream()
                    .sorted()
                    .map(String::valueOf)
                    .collect(Collectors.joining(StorageConstants.ENGLISH_COMMA)));
        }
    }
}
