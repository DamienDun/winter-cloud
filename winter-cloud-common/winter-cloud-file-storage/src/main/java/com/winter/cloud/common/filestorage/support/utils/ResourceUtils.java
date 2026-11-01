package com.winter.cloud.common.filestorage.support.utils;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.File;
import java.io.IOException;

/** 资源路径工具 */
public final class ResourceUtils {

    private ResourceUtils() {
    }

    public static String getResourceRootPath() {
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver()
                    .getResources(ResourceLoader.CLASSPATH_URL_PREFIX);
            for (Resource resource : resources) {
                if (resource.exists()) {
                    String rootPath = resource.getFile().getAbsolutePath().replace("\\", "/");
                    File dir = new File(rootPath);
                    if (!dir.exists() || !dir.isDirectory()) {
                        dir.mkdirs();
                    }
                    return rootPath;
                }
            }
            return "";
        } catch (IOException e) {
            return "";
        }
    }
}