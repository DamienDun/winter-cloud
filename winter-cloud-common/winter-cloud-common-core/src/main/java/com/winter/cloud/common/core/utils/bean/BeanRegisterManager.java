package com.winter.cloud.common.core.utils.bean;

import com.winter.cloud.common.core.utils.AutoMapUtils;
import com.winter.cloud.common.core.utils.StringUtils;
import com.winter.cloud.common.core.utils.reflect.ReflectUtils;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.context.annotation.AnnotationScopeMetadataResolver;
import org.springframework.context.annotation.ScopeMetadata;
import org.springframework.context.annotation.ScopeMetadataResolver;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * bean 注册管理
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2022/8/29 8:51
 */
public class BeanRegisterManager {

    private final Environment environment;
    private final ResourceLoader resourceLoader;
    private final BeanDefinitionRegistry registry;

    private final BeanNameGenerator beanNameGenerator = new AnnotationBeanNameGenerator();
    private final ScopeMetadataResolver scopeMetadataResolver = new AnnotationScopeMetadataResolver();


    public BeanRegisterManager(Environment environment, ResourceLoader resourceLoader,
                               BeanDefinitionRegistry registry) {
        this.environment = environment;
        this.resourceLoader = resourceLoader;
        this.registry = registry;
    }

    public Environment getEnvironment() {
        return environment;
    }

    public ResourceLoader getResourceLoader() {
        return resourceLoader;
    }

    public BeanDefinitionRegistry getRegistry() {
        return registry;
    }

    /**
     * 生成 Bean 名称
     *
     * @param beanClass
     * @return
     */
    public String generateBeanName(Class<?> beanClass) {
        AnnotatedGenericBeanDefinition abd = new AnnotatedGenericBeanDefinition(beanClass);
        return this.beanNameGenerator.generateBeanName(abd, registry);
    }

    /**
     * 创建 Bean 定义
     *
     * @param beanClass      Bean类型
     * @param propertyValues 属性
     * @param initMethod     初始化函数
     * @param destroyMethod  上下文关闭函数
     * @return
     */
    public final GenericBeanDefinition createBeanDefinition(Class<?> beanClass, Map<String, Object> propertyValues,
                                                            String initMethod, String destroyMethod) {
        GenericBeanDefinition beanDef = new GenericBeanDefinition();
        beanDef.setBeanClass(beanClass);
        if (propertyValues != null) {
            MutablePropertyValues mpv = new MutablePropertyValues();
            Map<String, Method> methodMap = ReflectUtils.findPropertiesSetMethodMap(beanClass);
            for (Map.Entry<String, Object> entry : propertyValues.entrySet()) {
                if (methodMap.containsKey(entry.getKey())) {
                    mpv.add(entry.getKey(), entry.getValue());
                }
            }
            methodMap.clear();
            beanDef.setPropertyValues(mpv);
        }
        if (!StringUtils.isEmpty(initMethod)) {
            beanDef.setInitMethodName(initMethod);
        }
        if (!StringUtils.isEmpty(destroyMethod)) {
            beanDef.setDestroyMethodName(destroyMethod);
        }
        ScopeMetadata scopeMetadata = this.scopeMetadataResolver.resolveScopeMetadata(beanDef);
        beanDef.setScope(scopeMetadata.getScopeName());
        return beanDef;
    }

    /**
     * 注册Bean
     *
     * @param beanName       bean名称(为空则自动生成名称)
     * @param beanClass      Bean类型
     * @param propertyValues 属性
     * @param initMethod     初始化函数
     * @param destroyMethod  上下文关闭函数
     */
    public boolean registerBean(String beanName, Class<?> beanClass, Map<String, Object> propertyValues,
                                String initMethod, String destroyMethod) {
        GenericBeanDefinition beanDef = createBeanDefinition(beanClass, propertyValues, initMethod, destroyMethod);
        if (StringUtils.isEmpty(beanName)) {
            beanName = this.beanNameGenerator.generateBeanName(beanDef, registry);
        }
        return registerBean(beanName, beanDef);
    }

    /**
     * 注册Bean
     *
     * @param beanName       bean名称(为空则自动生成名称)
     * @param beanDefinition Bean 定义
     */
    public final boolean registerBean(String beanName, GenericBeanDefinition beanDefinition) {
        if (StringUtils.isEmpty(beanName)) {
            beanName = this.beanNameGenerator.generateBeanName(beanDefinition, registry);
        }
        if (!registry.containsBeanDefinition(beanName)) {
            BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(beanDefinition, beanName);
            BeanDefinitionReaderUtils.registerBeanDefinition(definitionHolder, registry);
            return true;
        }
        return false;
    }

    /**
     * 注册Bean
     *
     * @param beanName  bean名称(为空则自动生成名称)
     * @param beanClass Bean类型
     */
    public final boolean registerBean(String beanName, Class<?> beanClass) {
        return registerBean(beanName, beanClass, null, null, null);
    }

    /**
     * 获取环境属性Map
     *
     * @param propertiesPrefix 属性前缀
     * @return 返回更新过的属性, 即中间带有减号的配置
     */
//    public final Map<String, Object> getEnvironmentPropertiesMap(String propertiesPrefix) {
//        if (this.getEnvironment() == null) {
//            return new HashMap<>(16);
//        }
//        return updateMapProperties(
//                new RelaxedPropertyResolver(this.getEnvironment(), propertiesPrefix).getSubProperties("."));
//    }
//
//    /**
//     * 获取环境属性
//     *
//     * @param propertiesPrefix 属性前缀
//     * @param propertiesClass  属性类型
//     * @return
//     */
//    public <E> E getEnvironmentProperties(String propertiesPrefix, Class<E> propertiesClass) {
//        return getEnvironmentProperties(getEnvironmentPropertiesMap(propertiesPrefix), propertiesClass);
//    }

    /**
     * 获取环境属性
     *
     * @param mapProperties   Map 属性
     * @param propertiesClass 属性类型
     * @return
     */
    public <E> E getEnvironmentProperties(Map<String, Object> mapProperties, Class<E> propertiesClass) {
        return AutoMapUtils.map(mapProperties, propertiesClass);
    }

    /**
     * 更新 Map 属性
     *
     * @param mapProperties 原属性
     * @return 将带有-符合去掉，并返回新的 Map
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> updateMapProperties(Map<String, Object> mapProperties) {
        Map<String, Object> map = new HashMap<>(mapProperties);
        for (Map.Entry<String, Object> entry : mapProperties.entrySet()) {
            Object value = entry.getValue();
            String key = entry.getKey();
            map.remove(key);
            if (value != null) {
                if (value instanceof Map) {
                    value = updateMapProperties((Map<String, Object>) value);
                }
                map.put(StringUtils.configurePropertieName(key), value);
            }
        }
        return map;
    }

}
