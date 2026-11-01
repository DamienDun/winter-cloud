package com.winter.cloud.common.filestorage.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.winter.cloud.common.filestorage.support.domain.StorageBaseEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 大文件上传任务
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2023/12/14 8:22
 */
@Getter
@Setter
@NoArgsConstructor
@TableName("fs_big_file_upload_task")
public class FsBigFileUploadTask extends StorageBaseEntity {

    /**
     * 分片上传的uploadId
     */
    private String uploadId;

    /**
     * 文件的Md5值
     */
    private String fileMd5;

    /**
     * 文件类型
     */
    private String fileType;

    /**
     * 桶名称
     */
    private String bucketName;

    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 访问路径
     */
    private String accessPath;

    /**
     * 文件总字节数
     */
    private Long totalBytes;

    /**
     * 已完成字节数
     */
    private Long completeBytes;

    /**
     * 分片字节数
     */
    private Long partBytes;

    /**
     * 分片数量
     */
    private Integer partCount;

    /**
     * 已上传的分片编号
     */
    private String uploadedPartNumbers;

    /**
     * 任务状态;0:取消上传;1:新建;2:上传中;3:上传完成
     */
    private Integer status;
}
