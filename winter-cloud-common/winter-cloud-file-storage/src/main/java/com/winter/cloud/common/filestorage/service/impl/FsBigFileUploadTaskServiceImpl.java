package com.winter.cloud.common.filestorage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.winter.cloud.common.core.utils.AutoMapUtils;
import com.winter.cloud.common.filestorage.domain.FsBigFileUploadTask;
import com.winter.cloud.common.filestorage.dto.FsBigFileUploadTaskInput;
import com.winter.cloud.common.filestorage.enums.FsBigFileUploadTaskStatusEnum;
import com.winter.cloud.common.filestorage.mapper.FsBigFileUploadTaskMapper;
import com.winter.cloud.common.filestorage.service.FsBigFileUploadTaskService;
import com.winter.cloud.common.filestorage.support.constant.StorageConstants;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 大文件上传任务服务实现
 *
 * @author Damien
 */
public class FsBigFileUploadTaskServiceImpl implements FsBigFileUploadTaskService {

    private final FsBigFileUploadTaskMapper fsBigFileUploadTaskMapper;

    public FsBigFileUploadTaskServiceImpl(FsBigFileUploadTaskMapper fsBigFileUploadTaskMapper) {
        this.fsBigFileUploadTaskMapper = fsBigFileUploadTaskMapper;
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
