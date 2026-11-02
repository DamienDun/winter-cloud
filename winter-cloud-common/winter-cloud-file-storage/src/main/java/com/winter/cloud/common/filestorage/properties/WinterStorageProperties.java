package com.winter.cloud.common.filestorage.properties;

import com.winter.cloud.common.core.config.WinterConfig;
import com.winter.cloud.common.filestorage.clients.aliyun.AliyunStorageClientProperties;
import com.winter.cloud.common.filestorage.clients.fastdfs.FastDFSStorageClientProperties;
import com.winter.cloud.common.filestorage.clients.huawei.HuaWeiStorageClientProperties;
import com.winter.cloud.common.filestorage.clients.local.LocalStorageClientProperties;
import com.winter.cloud.common.filestorage.clients.minio.MinioStorageClientProperties;
import com.winter.cloud.common.filestorage.clients.tencent.TencentStorageClientProperties;
import com.winter.cloud.common.filestorage.clients.aws.AwsStorageClientProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.io.Serializable;

/**
 * 存储属性
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2022/8/15 19:25
 */
@ConfigurationProperties(prefix = WinterStorageProperties.PREFIX)
@ToString(callSuper = true)
@Getter
@Setter
public class WinterStorageProperties implements Serializable {

    private static final long serialVersionUID = 5844534354126069479L;

    /**
     * 属性前缀
     */
    public final static String PREFIX = WinterConfig.PREFIX + ".storage.client";

    /**
     * 阿里云属性
     */
    private AliyunStorageClientProperties aliyun = new AliyunStorageClientProperties();

    /**
     * 华为云属性
     */
    private HuaWeiStorageClientProperties huawei = new HuaWeiStorageClientProperties();

    /**
     * 腾讯云属性
     */
    private TencentStorageClientProperties tencent = new TencentStorageClientProperties();


    /**
     * FastDFS 属性
     */
    private FastDFSStorageClientProperties fastDFS = new FastDFSStorageClientProperties();

    /**
     * 本地存储属性
     */
    private LocalStorageClientProperties local = new LocalStorageClientProperties();

    /**
     * Minio 属性
     */
    private MinioStorageClientProperties minio = new MinioStorageClientProperties();

    /**
     * AWS S3 对象存储属性
     */
    private AwsStorageClientProperties aws = new AwsStorageClientProperties();

    /**
     * 最大同时大文件上传数量
     */
    private int uploadBigFileCount = 10000;

    /**
     * 普通上传/下载默认存储通道（Minio / local / fastDFS / Aws / aliyunOOS / HuaWeiOBS / TencentCOS）。
     * 为空时若仅启用一个通道则自动选用；启用多个时必须配置。
     */
    private String defaultChannel;

    /**
     * 大文件分片使用的存储通道（Minio / Aws / aliyunOOS / HuaWeiOBS / TencentCOS）。
     * 为空则回退 defaultChannel；也可在申请接口传入 channelId 覆盖。
     */
    private String multipartChannel;

    /**
     *
     */
    public WinterStorageProperties() {

    }

}
