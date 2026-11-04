package com.winter.cloud.common.datasource.info;

import com.alibaba.druid.util.JdbcConstants;
import com.winter.cloud.common.core.constant.Constants;
import com.winter.cloud.common.core.utils.StringUtils;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 数据源信息
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2023/4/20 13:26
 */
@Getter
@Setter
@NoArgsConstructor
public class DatasourceInfo {

    /**
     * 数据源唯一标识
     */
    private String key;

    /**
     * 数据源名称
     */
    private String datasourceName;

    /**
     * 数据源类型
     */
    private String datasourceType;

    /**
     * 数据库
     */
    private String database;

    /**
     * jdbc用户名
     */
    private String jdbcUsername;

    /**
     * jdbc密码
     */
    private String jdbcPassword;

    /**
     * jdbc url
     */
    private String jdbcUrl;

    /**
     * jdbc 驱动
     */
    private String jdbcDriverClass;

    /**
     * 是否放弃缓存，每次获取新的 DataSource
     */
    private boolean disableCache;

    /**
     * 缓存时间
     */
    private long cacheTime;

    /**
     * 是否AES加密了密码
     */
    private boolean aesEncryptPassword = true;

    private int maxActive = 20;
    private int initialSize = 5;
    private long maxWait = 60000L;
    private long timeBetweenEvictionRunsMillis = 60000L;
    private long minEvictableIdleTimeMillis = 30000L;
    private boolean testWhileIdle = true;
    private boolean testOnBorrow;
    private boolean testOnReturn;
    private boolean breakAfterAcquireFailure = true;
    private int connectionErrorRetryAttempts = 1;
    private String validationQuery;
    private int connectTimeout = 30000;
    private int socketTimeout = 60000;

    public String getKey() {
        if (StringUtils.isNotEmpty(key)) {
            return key;
        }
        return datasourceType + Constants.COLON + datasourceName;
    }

    public String getValidationQuery() {
        if (StringUtils.isEmpty(validationQuery)) {
            return JdbcConstants.ORACLE.equals(getDatasourceType()) ? "SELECT 1 FROM DUAL" : "SELECT 1";
        }
        return validationQuery;
    }
}
