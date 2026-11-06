package com.winter.cloud.xxljob.core.util;

import com.winter.cloud.xxljob.core.conf.XxlJobAdminConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * i18n util (null-safe before XxlJobAdminConfig ready)
 */
public class I18nUtil {
    private static Logger logger = LoggerFactory.getLogger(I18nUtil.class);

    private static Properties prop = null;

    public static Properties loadI18nProp() {
        if (prop != null) {
            return prop;
        }
        try {
            // Spring may classload enums before XxlJobAdminConfig.afterPropertiesSet
            String i18n = "zh_CN";
            XxlJobAdminConfig adminConfig = XxlJobAdminConfig.getAdminConfig();
            if (adminConfig != null && adminConfig.getI18n() != null && adminConfig.getI18n().trim().length() > 0) {
                i18n = adminConfig.getI18n();
            }
            String i18nFile = MessageFormat.format("i18n/message_{0}.properties", i18n);
            Resource resource = new ClassPathResource(i18nFile);
            EncodedResource encodedResource = new EncodedResource(resource, "UTF-8");
            prop = PropertiesLoaderUtils.loadProperties(encodedResource);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            prop = new Properties();
        }
        return prop;
    }

    public static String getString(String key) {
        Properties p = loadI18nProp();
        if (p == null) {
            return key;
        }
        String val = p.getProperty(key);
        return val != null ? val : key;
    }

    public static String getMultString(String... keys) {
        Map<String, String> map = new HashMap<String, String>();
        Properties prop = loadI18nProp();
        if (keys != null && keys.length > 0) {
            for (String key : keys) {
                map.put(key, prop.getProperty(key));
            }
        } else {
            for (String key : prop.stringPropertyNames()) {
                map.put(key, prop.getProperty(key));
            }
        }
        return JacksonUtil.writeValueAsString(map);
    }
}