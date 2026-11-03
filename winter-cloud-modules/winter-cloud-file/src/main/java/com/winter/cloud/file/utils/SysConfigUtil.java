package com.winter.cloud.file.utils;

import com.winter.cloud.common.core.config.WinterConfig;
import com.winter.cloud.common.core.constant.CacheConstants;
import com.winter.cloud.common.core.constant.SecurityConstants;
import com.winter.cloud.common.core.domain.R;
import com.winter.cloud.common.core.text.Convert;
import com.winter.cloud.common.core.utils.SpringUtils;
import com.winter.cloud.common.core.utils.StringUtils;
import com.winter.cloud.common.redis.service.RedisService;
import com.winter.cloud.system.api.RemoteConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 读取系统参数：Redis -> Feign(system) -> 本地 JDBC(sys_config)
 */
public final class SysConfigUtil {

    private static final Logger log = LoggerFactory.getLogger(SysConfigUtil.class);

    private SysConfigUtil() {
    }

    /**
     * 文件上传-图片日期水印（未配置时默认开启）
     */
    public static boolean sysUploadPicWatermark() {
        String configValue = selectConfigByKey("sys.upload.picWatermark");
        return StringUtils.isEmpty(configValue) || Boolean.parseBoolean(configValue);
    }

    public static String selectConfigByKey(String configKey) {
        if (StringUtils.isEmpty(configKey)) {
            return StringUtils.EMPTY;
        }

        String cacheValue = getFromRedis(configKey);
        if (StringUtils.isNotEmpty(cacheValue)) {
            return cacheValue;
        }

        String remoteValue = getFromRemote(configKey);
        if (StringUtils.isNotEmpty(remoteValue)) {
            return remoteValue;
        }

        return getFromDb(configKey);
    }

    private static String getFromRedis(String configKey) {
        try {
            RedisService redisService = SpringUtils.getBean(RedisService.class);
            // 与 winter-system 一致：sys_config:{key}
            String value = Convert.toStr(redisService.getCacheObject(CacheConstants.SYS_CONFIG_KEY + configKey));
            if (StringUtils.isNotEmpty(value)) {
                return value;
            }
            // 兼容 portal 风格：{winter.name}:sys_config:{key}
            String name = WinterConfig.getName();
            if (StringUtils.isNotEmpty(name)) {
                value = Convert.toStr(redisService.getCacheObject(WinterConfig.joinKey(CacheConstants.SYS_CONFIG_KEY) + configKey));
                if (StringUtils.isNotEmpty(value)) {
                    return value;
                }
            }
        } catch (Exception e) {
            log.warn("读取系统参数 Redis 失败, key={}: {}", configKey, e.getMessage());
        }
        return StringUtils.EMPTY;
    }

    private static String getFromRemote(String configKey) {
        try {
            RemoteConfigService remoteConfigService = SpringUtils.getBean(RemoteConfigService.class);
            R<String> result = remoteConfigService.getConfigKey(configKey, SecurityConstants.INNER);
            if (result != null && R.isSuccess(result) && StringUtils.isNotEmpty(result.getData())) {
                return result.getData();
            }
            log.warn("Feign 获取系统参数失败, key={}, msg={}", configKey, result != null ? result.getMsg() : "null");
        } catch (Exception e) {
            log.warn("Feign 获取系统参数异常, key={}: {}", configKey, e.getMessage());
        }
        return StringUtils.EMPTY;
    }

    /**
     * 最后兜底：文件服务与 system 共用库时直接查 sys_config
     */
    private static String getFromDb(String configKey) {
        try {
            JdbcTemplate jdbcTemplate = SpringUtils.getBean(JdbcTemplate.class);
            String value = jdbcTemplate.query(
                    "select config_value from sys_config where config_key = ? limit 1",
                    rs -> rs.next() ? rs.getString(1) : null,
                    configKey);
            if (StringUtils.isNotEmpty(value)) {
                // 回填 Redis，后续命中缓存
                try {
                    SpringUtils.getBean(RedisService.class)
                            .setCacheObject(CacheConstants.SYS_CONFIG_KEY + configKey, value);
                } catch (Exception ignored) {
                    // ignore
                }
                return value;
            }
            log.warn("sys_config 无此参数, key={}", configKey);
        } catch (Exception e) {
            log.warn("JDBC 读取系统参数失败, key={}: {}", configKey, e.getMessage());
        }
        return StringUtils.EMPTY;
    }
}
