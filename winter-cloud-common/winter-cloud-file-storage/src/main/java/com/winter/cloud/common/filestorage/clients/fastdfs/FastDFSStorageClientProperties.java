package com.winter.cloud.common.filestorage.clients.fastdfs;

import com.winter.cloud.common.filestorage.properties.AbstractStorageClientProperties;
import com.winter.cloud.common.filestorage.properties.WinterStorageProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.Properties;

/**
 * FastDFS storage client properties (no SDK types on classpath for optional deps).
 */
@ToString(callSuper = true)
@Getter
@Setter
public class FastDFSStorageClientProperties extends AbstractStorageClientProperties {

    private static final long serialVersionUID = -1542557881330522318L;

    public static final String BEAN_CONDITIONAL_PROPERTY = WinterStorageProperties.PREFIX + ".fastDFS.enable";

    public static final String CHANNEL_BEAN_NAME = CHANNEL_BEAN_PREFIX + "FastDFS" + CHANNEL_BEAN_SUFFIX;

    /** example: 10.0.11.201:22122,10.0.11.202:22122 */
    private String trackerServers;

    private int connectTimeoutSeconds = 5;

    private int networkTimeoutSeconds = 30;

    private String charset = "UTF-8";

    private FastDFSHttp http = new FastDFSHttp();

    /**
     * Keys align with org.csource.fastdfs.ClientGlobal PROP_KEY_* constants.
     */
    public Properties createFastDFSProperties() {
        if (this.getHttp() == null) {
            this.setHttp(new FastDFSHttp());
        }
        Properties properties = new Properties();
        properties.put("fastdfs.tracker_servers", this.getTrackerServers());
        properties.put("fastdfs.connect_timeout_in_seconds", this.getConnectTimeoutSeconds());
        properties.put("fastdfs.network_timeout_in_seconds", this.getNetworkTimeoutSeconds());
        properties.put("fastdfs.charset", this.getCharset());
        properties.put("fastdfs.http_anti_steal_token", this.getHttp().isAntiStealToken());
        properties.put("fastdfs.http_secret_key", this.getHttp().getSecretKey());
        properties.put("fastdfs.http_tracker_http_port", this.getHttp().getTrackerHttpPort());
        return properties;
    }

    @ToString(callSuper = true)
    @Getter
    @Setter
    public static class FastDFSHttp implements Serializable {

        private static final long serialVersionUID = 1665790660513316844L;

        private int trackerHttpPort = 8080;

        private boolean antiStealToken = false;

        private String secretKey = "FastDFS1234567890";
    }
}