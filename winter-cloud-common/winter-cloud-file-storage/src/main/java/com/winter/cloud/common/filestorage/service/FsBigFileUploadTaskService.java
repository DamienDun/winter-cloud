package com.winter.cloud.common.filestorage.service;

import com.winter.cloud.common.filestorage.domain.FsBigFileUploadTask;
import com.winter.cloud.common.filestorage.dto.FsBigFileUploadTaskInput;

import java.util.List;
import java.util.Set;

/**
 * 大文件上传任务服务（上传过程 CRUD + 管理端列表/删除）
 *
 * @author Damien
 */
public interface FsBigFileUploadTaskService {

    /**
     * 新增任务
     */
    FsBigFileUploadTaskInput add(FsBigFileUploadTaskInput input);

    /**
     * 更新完成字节与状态
     */
    void updateCompleteBytesAndStatus(Long id, Long completeBytes, Integer status, Set<Integer> uploadedPartNumberList);

    /**
     * 按 uploadId 更新完成字节与状态
     */
    void updateCompleteBytesAndStatus(String uploadId, Long completeBytes, Integer status, Set<Integer> uploadedPartNumberList);

    /**
     * 根据文件 md5 查询
     */
    FsBigFileUploadTaskInput queryByFileMd5(String fileMd5);

    /**
     * 根据 uploadId 查询
     */
    FsBigFileUploadTaskInput queryByUploadId(String uploadId);

    /**
     * 取消任务
     */
    void cancel(Long id);

    /**
     * 管理端条件列表（配合 PageHelper）
     */
    List<FsBigFileUploadTask> selectTaskList(FsBigFileUploadTask query);

    /**
     * 按主键查询实体
     */
    FsBigFileUploadTask selectById(Long id);

    /**
     * 按主键批量删除：未完成中止分片，已完成删对象，最后删任务行
     */
    int deleteByIds(Long[] ids);
}
