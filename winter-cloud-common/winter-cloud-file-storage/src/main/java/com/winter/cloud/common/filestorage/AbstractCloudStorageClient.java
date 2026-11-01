package com.winter.cloud.common.filestorage;

import com.winter.cloud.common.core.utils.ExceptionUtil;
import com.winter.cloud.common.core.utils.StringUtils;
import com.winter.cloud.common.filestorage.properties.AbstractStorageKeyClientProperties;

/**
 * 云存储客户端
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2022/8/17 11:15
 */
public abstract class AbstractCloudStorageClient<TBucket extends AbstractBucket>
        extends AbstractStorageClient<TBucket> {

    private final String accessKey;
    private final String secretKey;
    private final String httpProtocol;
    private final String httpDomainName;

    /**
     * AbstractCloudStorageClient
     *
     * @param endpoint
     * @param defaultBucketName
     * @param accessKey
     * @param secretKey
     */
    public AbstractCloudStorageClient(String endpoint, String defaultBucketName,
                                      String accessKey, String secretKey) {
        super(StringUtils.removeALLWhitespace(endpoint), defaultBucketName);
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        int index = this.getEndpoint().indexOf("//");
        if (index > 2) {
            this.httpProtocol = this.getEndpoint().substring(0, index - 1);
            this.httpDomainName = this.getEndpoint().substring(index + 2);
        } else {
            this.httpProtocol = "http";
            this.httpDomainName = this.getEndpoint();
        }
    }

    /**
     * MinioStorageClient
     *
     * @param properties 属性
     */
    public AbstractCloudStorageClient(AbstractStorageKeyClientProperties properties) {
        this(properties.getEndpoint(),
                properties.getDefaultBucketName(),
                properties.getAccessKey(),
                properties.getSecretKey());
        this.setStorageClientProperties(properties);
    }

    /**
     * 获取访问键
     *
     * @return
     */
    public String getAccessKey() {
        return this.accessKey;
    }

    /**
     * 获取访问密钥
     *
     * @return
     */
    public String getSecretKey() {
        return this.secretKey;
    }

    /**
     * 获取Http协议
     *
     * @return
     */
    public String getHttpProtocol() {
        return this.httpProtocol;
    }

    /**
     * 获取Http域名
     *
     * @return
     */
    public String getHttpDomainName() {
        return this.httpDomainName;
    }

    /**
     * 获取访问Url
     *
     * @param bucketName 分区名称
     * @param fileInfo
     * @return
     */
    protected String getAccessUrl(String bucketName, FileInfo fileInfo) {
        String domainName = this.getHttpProtocol() + "://" + bucketName + "." + this.getHttpDomainName();
        return this.getPathAddress(domainName, fileInfo.getFullPath());
    }

    @Override
    public String getAccessUrl(String bucketName, String fullPath) {
        bucketName = this.checkBucketName(bucketName);
        ExceptionUtil.checkNotNullOrBlank(fullPath, "fullPath");
        FileInfo fileInfo = new FileInfo(fullPath, true);
        return this.getAccessUrl(this.checkBucketName(bucketName), fileInfo);
    }

}
