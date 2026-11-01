package com.winter.cloud.common.web.configure;

import com.winter.cloud.common.core.utils.StringUtils;
import com.winter.cloud.common.core.utils.bean.BeanRegisterManager;
import com.winter.cloud.common.core.utils.bean.BeanScanUtils;
import com.winter.cloud.common.web.handlers.ApiResponseBodyHandler;
import com.winter.cloud.common.web.annotation.ApiResponseBodyScan;
import com.winter.cloud.common.web.annotation.EnableWinterApiResponseBody;
import com.winter.cloud.common.web.handlers.ApiRequestMappingInfoHandler;
import com.winter.cloud.common.web.handlers.impl.ApiRequestMappingInfoHandlerImpl;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Api 响应体扫描配置
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2022/12/16 14:31
 */
public class ApiResponseBodyHandlerRegistrar implements ImportBeanDefinitionRegistrar, ResourceLoaderAware, EnvironmentAware {

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

    @Override
    public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {

        AnnotationAttributes annoAttrs = AnnotationAttributes.fromMap(metadata.getAnnotationAttributes(EnableWinterApiResponseBody.class.getName()));
        if (annoAttrs == null) {
            return;
        }

        Set<String> paks = createPackages(metadata);
        String[] arr = annoAttrs.getStringArray(EnableWinterApiResponseBody.API_CONTROLLER_PACKAGES_ATTRIBUTE_NAME);
        if (arr.length > 0) {
            for (String s : arr) {
                if (StringUtils.isNotEmpty(s)) {
                    paks.add(s);
                }
            }
        }

        BeanRegisterManager regManager = new BeanRegisterManager(this.getEnvironment(), this.getResourceLoader(), registry);

        String apiRequestBeanName = regManager.generateBeanName(ApiRequestMappingInfoHandler.class);
        GenericBeanDefinition definition;

        if (!registry.containsBeanDefinition(apiRequestBeanName)) {
            definition = regManager.createBeanDefinition(ApiRequestMappingInfoHandlerImpl.class, null, null, null);
            definition.getConstructorArgumentValues().addGenericArgumentValue(paks.toArray(new String[0]));
            regManager.registerBean(apiRequestBeanName, definition);
        }
        String apiResponseBeanName = regManager.generateBeanName(ApiResponseBodyHandler.class);
        if (!registry.containsBeanDefinition(apiResponseBeanName)) {
            definition = regManager.createBeanDefinition(ApiResponseBodyHandler.class, null, null, null);
            definition.getConstructorArgumentValues().addGenericArgumentValue(new RuntimeBeanReference(apiRequestBeanName));
            regManager.registerBean(apiResponseBeanName, definition);
        }
    }

    /**
     * 扫描指定的包
     *
     * @param metadata
     * @return
     */
    private Set<String> createPackages(AnnotationMetadata metadata) {
        Set<ApiResponseBodyScan> scans = BeanScanUtils.findStartupAnnotations(metadata, ApiResponseBodyScan.class);
        Set<String> paks = new LinkedHashSet<>(scans.size());
        for (ApiResponseBodyScan scan : scans) {
            for (String s : scan.value()) {
                if (StringUtils.isNotEmpty(s)) {
                    paks.add(s);
                }
            }
        }
        return paks;
    }
}

