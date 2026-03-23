package com.winter.cloud.common.swagger.config;


import com.winter.cloud.common.core.utils.PackageUtils;
import com.winter.cloud.common.core.utils.bean.BeanRegisterManager;
import com.winter.cloud.common.core.utils.bean.BeanScanUtils;
import com.winter.cloud.common.swagger.annotation.*;
import com.winter.cloud.common.swagger.models.WinterSwaggerApiGroupInfo;
import com.winter.cloud.common.swagger.models.WinterSwaggerApiHeaderParameterInfo;
import com.winter.cloud.common.swagger.models.WinterSwaggerApiInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Swagger 配置 扫描
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2022/8/17 15:40
 */
public class WinterSwaggerRegistrar implements ImportBeanDefinitionRegistrar, ResourceLoaderAware, EnvironmentAware {

    private static final Logger log = LoggerFactory.getLogger(WinterSwaggerRegistrar.class);

    private ResourceLoader resourceLoader;

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public ResourceLoader getResourceLoader() {
        return resourceLoader;
    }

    public Environment getEnvironment() {
        return environment;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {

        AnnotationAttributes annoAttrs = AnnotationAttributes.fromMap(metadata.getAnnotationAttributes(EnableWinterSwagger.class.getName()));
        if (annoAttrs == null) {
            return;
        }
        Map<String, Object> propertyValues = createApiInfoPropertyValues(metadata, annoAttrs);
        BeanRegisterManager regManager = new BeanRegisterManager(this.getEnvironment(), this.getResourceLoader(), registry);
        GenericBeanDefinition beanDefinition = regManager.createBeanDefinition(WinterSwaggerApiInfo.class, propertyValues, null, null);
        regManager.registerBean(null, beanDefinition);
        log.info("开始注册 WinterSwagger 文档...");
        List<WinterSwaggerApiGroupInfo> groupInfos = (List<WinterSwaggerApiGroupInfo>) propertyValues.get(EnableWinterSwagger.FIELD_GROUPS);
        for (WinterSwaggerApiGroupInfo group : groupInfos) {
            log.info("注册 WinterSwagger 文档组[{}]", group.getGroupName());
            BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(GroupedOpenApi.class,
                    // Lambda表达式：调用Builder模式创建对象
                    () -> GroupedOpenApi.builder()
                            .group(group.getGroupName())
                            .packagesToScan(group.getPackages().toArray(new String[group.getPackages().size()]))
                            .build()
            );
            beanDefinitionBuilder.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
            beanDefinitionBuilder.setLazyInit(true);
            registry.registerBeanDefinition(group.getBeanId(), beanDefinitionBuilder.getRawBeanDefinition());
        }
        log.info("Register WinterSwagger [{}]", groupInfos.stream().map(WinterSwaggerApiGroupInfo::getGroupName).collect(Collectors.joining("、")));
    }

    /**
     * 创建 ApiInfo 属性Map
     *
     * @param metadata
     * @param annoAttrs
     * @return
     */
    private Map<String, Object> createApiInfoPropertyValues(AnnotationMetadata metadata, AnnotationAttributes annoAttrs) {
        Map<String, Object> propertyValues = new HashMap<>(16);
        Map<String, WinterSwaggerApiGroupInfo> groupMap = createGroupMap(metadata);
        List<WinterSwaggerApiGroupInfo> groupInfos = createGroupInfos(annoAttrs, groupMap);
        List<WinterSwaggerApiHeaderParameterInfo> headerParameters = createHeaderParameters(annoAttrs);
        propertyValues.put(EnableWinterSwagger.FIELD_TITLE, annoAttrs.getString(EnableWinterSwagger.FIELD_TITLE));
        propertyValues.put(EnableWinterSwagger.FIELD_DESCRIPTION, annoAttrs.getString(EnableWinterSwagger.FIELD_DESCRIPTION));
        propertyValues.put(EnableWinterSwagger.FIELD_VERSION, annoAttrs.getString(EnableWinterSwagger.FIELD_VERSION));
        propertyValues.put(EnableWinterSwagger.FIELD_LICENSE, annoAttrs.getString(EnableWinterSwagger.FIELD_LICENSE));
        propertyValues.put(EnableWinterSwagger.FIELD_LICENSE_URL, annoAttrs.getString(EnableWinterSwagger.FIELD_LICENSE_URL));
        propertyValues.put(EnableWinterSwagger.FIELD_AUTHOR_NAME, annoAttrs.getString(EnableWinterSwagger.FIELD_AUTHOR_NAME));
        propertyValues.put(EnableWinterSwagger.FIELD_AUTHOR_URL, annoAttrs.getString(EnableWinterSwagger.FIELD_AUTHOR_URL));
        propertyValues.put(EnableWinterSwagger.FIELD_AUTHOR_EMAIL, annoAttrs.getString(EnableWinterSwagger.FIELD_AUTHOR_EMAIL));
        propertyValues.put(EnableWinterSwagger.FIELD_GROUPS, groupInfos);
        propertyValues.put(EnableWinterSwagger.FIELD_HEADER_PARAMETERS, headerParameters);
        return propertyValues;
    }

    private Map<String, WinterSwaggerApiGroupInfo> createGroupMap(AnnotationMetadata importingClassMetadata) {
        Set<WinterSwaggerScan> scanSubSet = BeanScanUtils.findStartupAnnotations(importingClassMetadata, WinterSwaggerScan.class);
        Set<WinterSwaggerScans> scanSet = BeanScanUtils.findStartupAnnotations(importingClassMetadata, WinterSwaggerScans.class);
        Map<String, WinterSwaggerApiGroupInfo> groupMap = new LinkedHashMap<>(scanSet.size());
        for (WinterSwaggerScans scans : scanSet) {
            if (scans.value() != null) {
                for (WinterSwaggerScan scan : scans.value()) {
                    this.swaggerScan(groupMap, scan);
                }
            }
        }
        for (WinterSwaggerScan scan : scanSubSet) {
            this.swaggerScan(groupMap, scan);
        }
        return groupMap;
    }

    private void swaggerScan(Map<String, WinterSwaggerApiGroupInfo> groupMap, WinterSwaggerScan scan) {
        WinterSwaggerApiGroupInfo groupInfo = groupMap.computeIfAbsent(scan.groupName(), key -> {
            WinterSwaggerApiGroupInfo group = new WinterSwaggerApiGroupInfo();
            if (scan.annotation() != null && !Annotation.class.equals(scan.annotation())) {
                group.setAnnotation(scan.annotation());
            } else {
                group.setAnnotation(null);
            }
            group.setGroupName(scan.groupName());
            group.setOrder(scan.order());
            return group;
        });
        if (scan.packages() != null && groupInfo.getAnnotation() == null) {
            String[] pakArray = PackageUtils.toPackages(scan.packages());
            for (String pak : pakArray) {
                groupInfo.getPackages().add(pak);
            }
        }
    }

    /**
     * 创建 API 分组信息
     *
     * @param annotationAttributes
     * @param groupMap
     * @return
     */
    @SuppressWarnings("unchecked")
    private List<WinterSwaggerApiGroupInfo> createGroupInfos(AnnotationAttributes annotationAttributes, Map<String, WinterSwaggerApiGroupInfo> groupMap) {
        AnnotationAttributes[] groupAttributes = annotationAttributes.getAnnotationArray(EnableWinterSwagger.FIELD_GROUPS);
        if (groupAttributes != null && groupAttributes.length > 0) {
            for (AnnotationAttributes groupAttribute : groupAttributes) {
                WinterSwaggerApiGroupInfo groupInfo = groupMap.computeIfAbsent(groupAttribute.getString(ApiGroup.FIELD_GROUP_NAME), key -> {
                    Object annotation = groupAttribute.get(ApiGroup.FIELD_ANNOTATION);
                    WinterSwaggerApiGroupInfo group = new WinterSwaggerApiGroupInfo();
                    if (annotation != null) {
                        Class<? extends Annotation> ann = (Class<? extends Annotation>) annotation;
                        if (!ann.equals(Annotation.class)) {
                            group.setAnnotation(ann);
                        } else {
                            group.setAnnotation(null);
                        }
                    } else {
                        group.setAnnotation(null);
                    }
                    group.setGroupName(key);
                    group.setOrder(groupAttribute.getNumber(ApiGroup.FIELD_ORDER));
                    return group;
                });
                if (groupInfo.getAnnotation() == null) {
                    String[] arrays = PackageUtils.toPackages(groupAttribute.getStringArray(ApiGroup.FIELD_PACKAGES));
                    if (arrays != null) {
                        for (String array : arrays) {
                            groupInfo.getPackages().add(array);
                        }
                    }
                }
            }
        }
        List<WinterSwaggerApiGroupInfo> list = new ArrayList<>(groupMap.values());
        list.sort(Comparator.comparingInt(WinterSwaggerApiGroupInfo::getOrder));
        groupMap.clear();
        return list;
    }

    /**
     * 创建 API 分组信息
     *
     * @param annotationAttributes 注解属性
     * @return API 分组信息
     */
    private List<WinterSwaggerApiHeaderParameterInfo> createHeaderParameters(AnnotationAttributes annotationAttributes) {
        List<WinterSwaggerApiHeaderParameterInfo> list;
        AnnotationAttributes[] groupAttributes = annotationAttributes
                .getAnnotationArray(EnableWinterSwagger.FIELD_HEADER_PARAMETERS);
        if (groupAttributes != null && groupAttributes.length > 0) {
            list = new ArrayList<>(groupAttributes.length);
            for (AnnotationAttributes groupAttribute : groupAttributes) {
                WinterSwaggerApiHeaderParameterInfo headerParameter = new WinterSwaggerApiHeaderParameterInfo();
                headerParameter.setName(groupAttribute.getString(ApiHeaderParameter.FIELD_NAME));
                headerParameter.setDataType(groupAttribute.getString(ApiHeaderParameter.FIELD_DATA_TYPE));
                headerParameter.setDescription(groupAttribute.getString(ApiHeaderParameter.FIELD_DESCRIPTION));
                headerParameter.setRequired(groupAttribute.getBoolean(ApiHeaderParameter.FIELD_REQUIRED));
                list.add(headerParameter);
            }
        } else {
            list = new ArrayList<>(0);
        }
        return list;
    }
}
