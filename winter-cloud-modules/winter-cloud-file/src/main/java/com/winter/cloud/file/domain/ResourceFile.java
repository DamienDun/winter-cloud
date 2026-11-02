package com.winter.cloud.file.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * 资源文件上传记录（对齐 portal ResourceFile）
 */
@Getter
@Setter
@NoArgsConstructor
@TableName("fs_resource_file")
public class ResourceFile implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 原始文件名 */
    private String originName;

    /** 文件类型;0其他;1图片;2音频;3视频;4pdf;5doc;6excel;7ppt;8txt;9html */
    private Integer fileType;

    /** 访问路径/完整URL */
    private String path;

    /** 相对对象路径 */
    private String relativePath;

    /** 文件大小(字节) */
    private Long length;

    /** 业务模块 */
    private String module;

    /** 文件MD5 */
    private String fileMd5;

    /** 申请上传次数 */
    private Integer applyUploadCount;

    /** 存储通道ID */
    private String channelId;

    /** 桶名 */
    private String bucketName;

    private String remark;

    private Date createTime;

    private Date updateTime;
}