package com.winter.cloud.common.datasource.util;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.util.JdbcConstants;
import com.alibaba.druid.util.JdbcUtils;
import com.winter.cloud.common.datasource.util.AESUtil;
import com.winter.cloud.common.datasource.info.DatasourceInfo;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * jdbc 工具类
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2023/4/20 13:38
 */
public class JdbcUtil implements JdbcConstants {

    private static final Logger logger = LoggerFactory.getLogger(JdbcUtil.class);

    /**
     * 默认缓存 4 小时（滑动过期：每次获取都会续期）
     */
    private static final long DEFAULT_CACHE_TIME = 4 * 60 * 60 * 1000L;

    /**
     * DataSource 本地缓存。同一 key 复用同一连接池，避免重复创建与误关仍被持有的实例。
     */
    private static final ConcurrentHashMap<String, DataSourceCache> DATA_SOURCE_CACHE = new ConcurrentHashMap<>();

    /**
     * 获取数据源
     *
     * @param datasourceInfo
     * @return
     * @throws SQLException
     */
    public static DataSource getDataSource(DatasourceInfo datasourceInfo) throws SQLException {
        // 放弃缓存：每次新建，由调用方在用完后通过 closeDataSource / AbstractDsQueryTool.close 关闭
        if (datasourceInfo.isDisableCache()) {
            return createDruidDataSource(datasourceInfo);
        }

        String key = datasourceInfo.getKey();
        long cacheTime = datasourceInfo.getCacheTime() > 0 ? datasourceInfo.getCacheTime() : DEFAULT_CACHE_TIME;
        long now = System.currentTimeMillis();

        DataSourceCache cached = DATA_SOURCE_CACHE.get(key);
        if (cached != null) {
            // 复用并滑动续期；不过期重建，避免关闭仍被 AbstractDsQueryTool 持有的池
            cached.expireTime = now + cacheTime;
            return cached.dataSource;
        }

        DruidDataSource newDataSource = createDruidDataSource(datasourceInfo);
        DataSourceCache newCache = new DataSourceCache(newDataSource, now + cacheTime);
        DataSourceCache previous = DATA_SOURCE_CACHE.putIfAbsent(key, newCache);
        if (previous != null) {
            // 并发下其它线程已放入，关闭本次新建的多余实例
            closeDataSource(newDataSource);
            previous.expireTime = now + cacheTime;
            return previous.dataSource;
        }
        return newDataSource;
    }

    /**
     * 创建 Druid 连接池。
     * 归还连接请使用 connection.close() / JdbcUtils.close(conn)，勿用 discardConnection（那是丢弃物理连接）。
     *
     * @param datasourceInfo
     * @return
     */
    private static DruidDataSource createDruidDataSource(DatasourceInfo datasourceInfo) throws SQLException {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUsername(datasourceInfo.getJdbcUsername());
        if (datasourceInfo.isAesEncryptPassword()) {
            dataSource.setPassword(AESUtil.decrypt(datasourceInfo.getJdbcPassword()));
        } else {
            dataSource.setPassword(datasourceInfo.getJdbcPassword());
        }
        dataSource.setUrl(datasourceInfo.getJdbcUrl());
        dataSource.setDriverClassName(datasourceInfo.getJdbcDriverClass());

        dataSource.setMaxActive(datasourceInfo.getMaxActive());
        dataSource.setInitialSize(datasourceInfo.getInitialSize());
        dataSource.setMaxWait(datasourceInfo.getMaxWait());
        dataSource.setTimeBetweenEvictionRunsMillis(datasourceInfo.getTimeBetweenEvictionRunsMillis());
        dataSource.setMinEvictableIdleTimeMillis(datasourceInfo.getMinEvictableIdleTimeMillis());
        dataSource.setTestWhileIdle(datasourceInfo.isTestWhileIdle());
        dataSource.setTestOnBorrow(datasourceInfo.isTestOnBorrow());
        dataSource.setTestOnReturn(datasourceInfo.isTestOnReturn());
        dataSource.setBreakAfterAcquireFailure(datasourceInfo.isBreakAfterAcquireFailure());
        dataSource.setConnectionErrorRetryAttempts(datasourceInfo.getConnectionErrorRetryAttempts());
        dataSource.setValidationQuery(datasourceInfo.getValidationQuery());
        dataSource.setConnectTimeout(datasourceInfo.getConnectTimeout());
        dataSource.setSocketTimeout(datasourceInfo.getSocketTimeout());
        return dataSource;
    }

    /**
     * 创建Hikari连接池
     *
     * @param datasourceInfo
     * @return
     */
    private static DataSource createHikariDataSource(DatasourceInfo datasourceInfo) throws SQLException {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setUsername(datasourceInfo.getJdbcUsername());
        if (datasourceInfo.isAesEncryptPassword()) {
            dataSource.setPassword(AESUtil.decrypt(datasourceInfo.getJdbcPassword()));
        } else {
            dataSource.setPassword(datasourceInfo.getJdbcPassword());
        }
        dataSource.setJdbcUrl(datasourceInfo.getJdbcUrl());
        dataSource.setDriverClassName(datasourceInfo.getJdbcDriverClass());
        dataSource.setMaximumPoolSize(1);
        dataSource.setMinimumIdle(0);
        dataSource.setConnectionTimeout(30000);
        return dataSource;
    }

    /**
     * 关闭数据源（关闭连接池，释放物理连接）
     */
    public static void closeDataSource(DataSource dataSource) {
        if (dataSource == null) {
            return;
        }
        if (dataSource instanceof AutoCloseable) {
            try {
                ((AutoCloseable) dataSource).close();
            } catch (Exception e) {
                logger.warn("close DataSource error", e);
            }
            return;
        }
        logger.warn("DataSource 未实现 AutoCloseable，无法自动关闭: {}", dataSource.getClass().getName());
    }

    /**
     * 按缓存 key 移除并关闭数据源（确认无业务再使用该池时调用）
     */
    public static void removeAndCloseDataSource(String key) {
        if (key == null) {
            return;
        }
        DataSourceCache removed = DATA_SOURCE_CACHE.remove(key);
        if (removed != null) {
            closeDataSource(removed.dataSource);
        }
    }

    /**
     * 清理已过期且未被续期的缓存连接池（确认对应数据源已无业务持有时再调用）
     */
    public static void cleanExpireDataSource() {
        long now = System.currentTimeMillis();
        for (Map.Entry<String, DataSourceCache> entry : DATA_SOURCE_CACHE.entrySet()) {
            DataSourceCache cache = entry.getValue();
            if (cache != null && now >= cache.expireTime) {
                if (DATA_SOURCE_CACHE.remove(entry.getKey(), cache)) {
                    closeDataSource(cache.dataSource);
                }
            }
        }
    }

    /**
     * 丢弃连接（不归还池，直接废弃物理连接）。仅在连接已损坏时使用。
     * 正常释放请用 JdbcUtils.close(conn)。
     */
    @Deprecated
    public static void discardConnection(DruidDataSource druidDataSource, Connection conn) {
        if (conn == null) {
            return;
        }
        if (druidDataSource == null) {
            JdbcUtils.close(conn);
            return;
        }
        try {
            druidDataSource.discardConnection(conn);
        } catch (Exception e) {
            logger.debug("discard druid connection error", e);
        }
    }

    private static class DataSourceCache {
        private final DataSource dataSource;
        private volatile long expireTime;

        private DataSourceCache(DataSource dataSource, long expireTime) {
            this.dataSource = dataSource;
            this.expireTime = expireTime;
        }
    }
}
