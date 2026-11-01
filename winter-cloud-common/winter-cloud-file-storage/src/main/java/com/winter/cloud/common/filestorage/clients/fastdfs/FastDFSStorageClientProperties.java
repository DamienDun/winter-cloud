package com.winter.cloud.common.filestorage.clients.fastdfs;

import com.winter.cloud.common.core.exception.ConfigureException;
import com.winter.cloud.common.filestorage.properties.AbstractStorageClientProperties;
import com.winter.cloud.common.filestorage.properties.WinterStorageProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.csource.fastdfs.ClientGlobal;

import java.io.Serializable;
import java.util.Properties;

/**
 * FastDFS 存储客户端属性
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2022/8/17 14:01
 */
@ToString(callSuper = true)
@Getter
@Setter
public class FastDFSStorageClientProperties extends AbstractStorageClientProperties {

    private static final long serialVersionUID = -1542557881330522318L;

    /**
     * bean条件属性
     */
    public static final String BEAN_CONDITIONAL_PROPERTY = WinterStorageProperties.PREFIX + ".fastDFS.enable";

    /**
     * 通道 Bean 名称
     */
    public static final String CHANNEL_BEAN_NAME = CHANNEL_BEAN_PREFIX + "FastDFS" + CHANNEL_BEAN_SUFFIX;

    /**
     * 跟踪服务器列表
     * <p>
     * 示例  10.0.11.201:22122,10.0.11.202:22122,10.0.11.203:22122
     * </p>
     */
    private String trackerServers;

    /**
     * 连接超时(秒)
     */
    private int connectTimeoutSeconds = 5;

    /**
     * 网络超时(秒)
     */
    private int networkTimeoutSeconds = 30;

    /**
     * 编码
     */
    private String charset = "UTF-8";

    /**
     * http配置
     */
    private FastDFSHttp http = new FastDFSHttp();


    /**
     * 创建FastDFSProperties属性
     *
     * @return
     */
    public Properties createFastDFSProperties() {
        if (this.getHttp() == null) {
            this.setHttp(new FastDFSHttp());
        }
        Properties properties = new Properties();
        properties.put(ClientGlobal.PROP_KEY_TRACKER_SERVERS, this.getTrackerServers());
        properties.put(ClientGlobal.PROP_KEY_CONNECT_TIMEOUT_IN_SECONDS, this.getConnectTimeoutSeconds());
        properties.put(ClientGlobal.PROP_KEY_NETWORK_TIMEOUT_IN_SECONDS, this.getNetworkTimeoutSeconds());
        properties.put(ClientGlobal.PROP_KEY_CHARSET, this.getCharset());
        properties.put(ClientGlobal.PROP_KEY_HTTP_ANTI_STEAL_TOKEN, this.getHttp().isAntiStealToken());
        properties.put(ClientGlobal.PROP_KEY_HTTP_SECRET_KEY, this.getHttp().getSecretKey());
        properties.put(ClientGlobal.PROP_KEY_HTTP_TRACKER_HTTP_PORT, this.getHttp().getTrackerHttpPort());
        return properties;
    }

    /**
     * 初始化
     */
    public void initByProperties() {
        try {
            ClientGlobal.initByProperties(this.createFastDFSProperties());
        } catch (Exception e) {
            throw new ConfigureException(e, "初始化FastDFS出错:" + e.getMessage());
        }
    }

    /**
     * Http配置
     */
    @ToString(callSuper = true)
    @Getter
    @Setter
    public static class FastDFSHttp implements Serializable {

        private static final long serialVersionUID = 1665790660513316844L;

        /**
         * 跟踪Http端口
         */
        private int trackerHttpPort = 8080;

        /**
         * 启用票据
         */
        private boolean antiStealToken = false;

        /**
         * 票据Key
         */
        private String secretKey = "FastDFS1234567890";

    }

}
