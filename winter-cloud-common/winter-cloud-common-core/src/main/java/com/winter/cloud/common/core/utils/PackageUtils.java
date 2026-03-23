package com.winter.cloud.common.core.utils;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.ClassUtils;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * 包 工具类
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2022/8/17 15:49
 */
public class PackageUtils {

    static final String DEFAULT_RESOURCE_PATTERN = "**/*.class";

    /**
     * 获取某包下特定注解的所有类型
     *
     * @param packageName     包名
     * @param annotationClass 注解类型
     * @return 类集
     * @throws IOException            IO异常
     * @throws ClassNotFoundException 类不存在异常
     */
    public static Set<Class<?>> getPackageAnnotationClass(String packageName,
                                                          Class<? extends Annotation> annotationClass) throws IOException, ClassNotFoundException {
        if (annotationClass == null) {
            throw new NullPointerException("annotationClass 为 null");
        }
        return getPackageClass(packageName, type -> isAnnotationClass(type, annotationClass));
    }

    /**
     * 获取某包下所有类型
     *
     * @param packageName 包名
     * @param predicate   条件
     * @return
     * @throws IOException            IO异常
     * @throws ClassNotFoundException 类不存在异常
     */
    public static Set<Class<?>> getPackageClass(String packageName, Predicate<Class<?>> predicate) throws IOException, ClassNotFoundException {
        Set<String> className = getClassName(packageName);
        Set<Class<?>> types = new HashSet<Class<?>>();
        for (String name : className) {
            if (StringUtils.isEmpty(name)) {
                continue;
            }
            Class<?> type = Class.forName(name);
            if (predicate == null || (predicate != null && predicate.test(type))) {
                types.add(type);
            }
        }
        return types;
    }

    /**
     * 获取某包下所有类
     *
     * @param packageName 包名
     * @return 类列表
     * @throws IOException
     */
    public static Set<String> getClassName(String packageName) throws IOException {
        if (StringUtils.isEmpty(packageName)) {
            throw new NullPointerException("packageName 为 null 或空字符串");
        }
        Set<String> fileNames = new HashSet<String>();
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory(resolver);
        String fullPackage = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX
                + ClassUtils.convertClassNameToResourcePath(packageName) + "/" + DEFAULT_RESOURCE_PATTERN;
        Resource[] resources = resolver.getResources(fullPackage);
        if (resources != null && resources.length > 0) {
            for (Resource resource : resources) {
                if (resource.isReadable()) {
                    MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(resource);
                    if (metadataReader != null) {
                        fileNames.add(metadataReader.getClassMetadata().getClassName());
                    }
                }
            }
        }
        return fileNames;
    }

    /**
     * 判断类是否包含特定的注解
     *
     * @param classType
     * @param annotationClass
     * @return
     */
    public static boolean isAnnotationClass(Class<?> classType, Class<? extends Annotation> annotationClass) {
        if (classType == null) {
            throw new NullPointerException("classType 为 null ");
        }
        if (annotationClass == null) {
            throw new NullPointerException("annotationClass 为 null");
        }
        return classType.getAnnotation(annotationClass) != null;
    }

    /**
     * 处理包中的分隔符
     *
     * @param packages
     * @return
     */
    public static String[] toPackages(String[] packages) {
        if (packages == null) {
            return new String[0];
        }
        List<String> list = new ArrayList<>();
        for (String pak : packages) {
            if (!StringUtils.isEmpty(pak)) {
                String[] paks = StringUtils.urlOrPackageToStringArray(pak);
                for (String s : paks) {
                    if (!list.contains(s)) {
                        list.add(s);
                    }
                }
            }
        }
        return list.toArray(new String[0]);
    }
}
