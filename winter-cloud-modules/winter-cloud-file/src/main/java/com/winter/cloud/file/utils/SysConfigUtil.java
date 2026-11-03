package com.winter.cloud.file.utils;

import com.winter.cloud.common.core.constant.CacheConstants;
import com.winter.cloud.common.core.text.Convert;
import com.winter.cloud.common.core.utils.SpringUtils;
import com.winter.cloud.common.core.utils.StringUtils;
import com.winter.cloud.common.redis.service.RedisService;

/**
 * 读取系统参数缓存（Redis key = sys_config:{configKey}，与 system 模块一致）
 */
public final class SysConfigUtil {

    private SysConfigUtil() {
    }

    /**
     * 文件上传-图片日期水印（未配置时默认开启）
     */
    public static boolean sysUploadPicWatermark() {
        String configValue = selectConfigByKey("sys.upload.picWatermark");
        return StringUtils.isEmpty(configValue) || Boolean.parseBoolean(configValue);
    }

    /**
     * 按参数键名读取 Redis：sys_config:{configKey}
     * 例：sys.upload.picWatermark -> sys_config:sys.upload.picWatermark
     */
    public static String selectConfigByKey(String configKey) {
        if (StringUtils.isEmpty(configKey)) {
            return StringUtils.EMPTY;
        }
        try {
            RedisService redisService = SpringUtils.getBean(RedisService.class);
            return Convert.toStr(redisService.getCacheObject(getCacheKey(configKey)), StringUtils.EMPTY);
        } catch (Exception e) {
            return StringUtils.EMPTY;
        }
    }

    private static String getCacheKey(String configKey) {
        return CacheConstants.SYS_CONFIG_KEY + configKey;
    }
}
