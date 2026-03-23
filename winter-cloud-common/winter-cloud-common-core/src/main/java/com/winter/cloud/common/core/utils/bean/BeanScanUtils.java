package com.winter.cloud.common.core.utils.bean;

import com.winter.cloud.common.core.exception.ConfigureException;
import com.winter.cloud.common.core.utils.PackageUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotationMetadata;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * bean 扫描工具类
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2022/8/29 8:50
 */
public class BeanScanUtils {

    /**
     * 根据注解元查找所在关联的注解
     *
     * @param annotationMetadata  解元
     * @param scanAnnotationClass 扫描注解类型
     * @param <TScan>             注解类型
     * @return
     */
    public static <TScan extends Annotation> Set<TScan> findStartupAnnotations(AnnotationMetadata annotationMetadata, Class<TScan> scanAnnotationClass) {
        try {
            return findStartupAnnotations(Class.forName(annotationMetadata.getClassName()), scanAnnotationClass);
        } catch (ClassNotFoundException e) {
            throw new ConfigureException(e, String.format("扫描[%s]出错:%s", annotationMetadata.getClassName(), e.getMessage()));
        }
    }

    /**
     * 根据启动类型查找所在关联的注解
     *
     * @param startupClass        启动类型
     * @param scanAnnotationClass 扫描注解类型
     * @param <TScan>             注解类型
     * @return
     */
    public static <TScan extends Annotation> Set<TScan> findStartupAnnotations(Class<?> startupClass, Class<TScan> scanAnnotationClass) {
        Set<Annotation> annotationSet = new HashSet<>(100);
        try {
            Set<TScan> scans = new LinkedHashSet<>(annotationSet.size());
            //扫描启动类和类上引用的关联类
            findScanAnnotations(startupClass, annotationSet, scans, scanAnnotationClass);
            Set<Class<?>> typeSet = null;
            try {
                //查找启动所在的包，并检查对应的 Configuration 配置
                typeSet = PackageUtils.getPackageAnnotationClass(startupClass.getPackage().getName(), Configuration.class);
            } catch (Exception e) {
                throw new ConfigureException(e, String.format("扫描[%s]出错:%s", startupClass.getName(), e.getMessage()));
            }
            for (Class<?> type : typeSet) {
                findScanAnnotations(type, annotationSet, scans, scanAnnotationClass);
            }
            return scans;
        } finally {
            annotationSet.clear();
        }
    }

    /**
     * 递归查找
     *
     * @param type                类型
     * @param annotationSet       重复处理排除集
     * @param scans               扫描集合
     * @param scanAnnotationClass 扫描注解类型
     * @param <TScan>             注解类型
     */
    private static <TScan extends Annotation> void findScanAnnotations(Class<?> type, Set<Annotation> annotationSet, Set<TScan> scans, Class<TScan> scanAnnotationClass) {
        TScan scanAnnotation = type.getAnnotation(scanAnnotationClass);
        if (scanAnnotation != null) {
            scans.add(scanAnnotation);
        }
        Annotation[] annotations = type.getAnnotations();
        for (Annotation annotation : annotations) {
            if (annotationSet.add(annotation)) {
                findScanAnnotations(annotation.annotationType(), annotationSet, scans, scanAnnotationClass);
            }
        }
        if (type.getSuperclass() != null && !type.getSuperclass().equals(Object.class)) {
            findScanAnnotations(type.getSuperclass(), annotationSet, scans, scanAnnotationClass);
        }
    }
}
