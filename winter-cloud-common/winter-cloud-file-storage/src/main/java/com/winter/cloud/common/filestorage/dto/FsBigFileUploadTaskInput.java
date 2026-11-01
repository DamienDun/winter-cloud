package com.winter.cloud.common.filestorage.dto;

import com.winter.cloud.common.filestorage.support.constant.StorageConstants;
import com.winter.cloud.common.filestorage.support.domain.PrimaryKey;
import com.winter.cloud.common.core.utils.StringUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2023/12/14 8:50
 */
@Getter
@Setter
@NoArgsConstructor
@Schema(description = "大文件上传任务入参")
public class FsBigFileUploadTaskInput implements PrimaryKey<Long> {

    private static final long serialVersionUID = -6906400844497727410L;

    /**
     * id
     */
    @Schema(description = "id")
    private Long id;

    /**
     * 分片上传的uploadId
     */
    @Schema(description = "分片上传的uploadId")
    private String uploadId;

    /**
     * 存储通道 ID
     */
    @Schema(description = "存储通道ID")
    private String channelId;

    /**
     * 文件的Md5值
     */
    @Schema(description = "文件的Md5值")
    private String fileMd5;

    /**
     * 文件类型
     */
    @Schema(description = "文件类型")
    private String fileType;

    /**
     * 桶名称
     */
    @Schema(description = "桶名称")
    private String bucketName;

    /**
     * 文件路径
     */
    @Schema(description = "文件路径")
    private String filePath;

    /**
     * 访问路径
     */
    @Schema(description = "访问路径")
    private String accessPath;

    /**
     * 文件总字节数
     */
    @Schema(description = "文件总字节数")
    private Long totalBytes;

    /**
     * 已完成字节数
     */
    @Schema(description = "已完成字节数")
    private Long completeBytes;

    /**
     * 分片字节数
     */
    @Schema(description = "分片字节数")
    private Long partBytes;

    /**
     * 分片数量
     */
    @Schema(description = "分片数量")
    private Integer partCount;

    /**
     * 已上传的分片编号
     */
    @Schema(description = "已上传的分片编号")
    private String uploadedPartNumbers;

    /**
     * 任务状态;0:取消上传;1:新建;2:上传中;3:上传完成
     */
    @Schema(description = "任务状态;0:取消上传;1:新建;2:上传中;3:上传完成")
    private Integer status;

    @Schema(description = "已上传的分块编号")
    private Set<Integer> uploadedPartNumberList = new HashSet<>();

    public Set<Integer> getUploadedPartNumberList() {
        if (StringUtils.isNotEmpty(uploadedPartNumbers)) {
            return Arrays.stream(uploadedPartNumbers.trim().split(StorageConstants.ENGLISH_COMMA))
                    .map(s -> Integer.valueOf(s.trim())).collect(Collectors.toSet());
        }
        return uploadedPartNumberList;
    }

    public String getUploadedPartNumbers() {
        if (!CollectionUtils.isEmpty(uploadedPartNumberList)) {
            return uploadedPartNumberList.stream().map(String::valueOf).collect(Collectors.joining(StorageConstants.ENGLISH_COMMA));
        }
        return uploadedPartNumbers;
    }
}
