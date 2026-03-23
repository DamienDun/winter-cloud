package com.winter.cloud.common.core.utils;


import com.winter.cloud.common.core.utils.function.FunctionTwoAction;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.modelmapper.TypeMap;
import org.modelmapper.convention.MatchingStrategies;
import org.modelmapper.convention.NameTransformers;
import org.modelmapper.convention.NamingConventions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Damien
 * @description bean list 转换工具
 * @create 2022/7/20 8:47
 */
public abstract class AutoMapUtils {

    private static final ModelMapper MODEL_MAPPER = new ModelMapper();

    static {
        MODEL_MAPPER.getConfiguration().setSourceNamingConvention(NamingConventions.JAVABEANS_ACCESSOR)
                .setDestinationNamingConvention(NamingConventions.JAVABEANS_MUTATOR)
                .setSourceNameTransformer(NameTransformers.JAVABEANS_ACCESSOR)
                .setDestinationNameTransformer(NameTransformers.JAVABEANS_MUTATOR)
                .setMatchingStrategy(MatchingStrategies.STRICT);
    }

    /**
     * 加载
     *
     * @param source      源对象
     * @param destination 被赋值的对象
     */
    public static void mapForLoad(Object source, Object destination) {
        if (source != null && destination != null) {
            MODEL_MAPPER.map(source, destination);
        }
    }


    /**
     * 转换为列表
     *
     * @param sourceList  源列表
     * @param targetClass 目标类型
     * @return 集合
     */
    public static <Source, Target> List<Target> mapForList(Collection<Source> sourceList, Class<Target> targetClass) {
        return mapForList(sourceList, targetClass, null);
    }

    /**
     * 转换为列表
     *
     * @param sourceList    源列表
     * @param targetClass   目标类型
     * @param convertAction 函数式编程-copy的属性名称不一样，可在这里单独设置
     * @return 集合
     */
    public static <Source, Target> List<Target> mapForList(Collection<Source> sourceList, Class<Target> targetClass,
                                                           FunctionTwoAction<Source, Target> convertAction) {
        if (sourceList == null) {
            return new ArrayList<>(16);
        }
        List<Target> items = new ArrayList<Target>(sourceList.size());
        if (convertAction != null) {
            for (Source source : sourceList) {
                Target item = map(source, targetClass);
                convertAction.apply(source, item);
                items.add(item);
            }
        } else {
            for (Source source : sourceList) {
                Target item = map(source, targetClass);
                items.add(item);
            }
        }
        return items;
    }

    /**
     * 转换
     *
     * @param source      源对象
     * @param targetClass 目标类型
     * @return 目标对象
     */
    public static <Target> Target map(Object source, Class<Target> targetClass) {
        if (source == null) {
            return null;
        }
        return MODEL_MAPPER.map(source, targetClass);
    }

    /**
     * 转换
     *
     * @param source        源对象
     * @param targetClass   目标类型
     * @param convertAction 函数式编程-copy的属性名称不一样，可在这里单独设置
     * @return 目标对象
     */
    public static <Target> Target map(Object source, Class<Target> targetClass, FunctionTwoAction<Object, Target> convertAction) {
        if (source == null) {
            return null;
        }
        Target result = MODEL_MAPPER.map(source, targetClass);
        if (convertAction != null) {
            convertAction.apply(source, result);
        }
        return result;
    }

    /**
     * 转换
     *
     * @param source      源
     * @param targetClass 目标类型
     * @param typeMapName
     * @return
     */
    public <Target> Target map(Object source, Class<Target> targetClass, String typeMapName) {
        if (source == null) {
            return null;
        }
        return MODEL_MAPPER.map(source, targetClass, typeMapName);
    }

    /**
     * 转换
     *
     * @param source        源
     * @param targetClass   目标类型
     * @param convertAction 函数式编程-copy的属性名称不一样，可在这里单独设置
     * @param typeMapName
     * @return
     */
    public <Target> Target map(Object source, Class<Target> targetClass, String typeMapName, FunctionTwoAction<Object, Target> convertAction) {
        if (source == null) {
            return null;
        }
        Target result = MODEL_MAPPER.map(source, targetClass, typeMapName);
        if (convertAction != null) {
            convertAction.apply(source, result);
        }
        return result;
    }

    /**
     * 添加转换
     *
     * @param converter 转换器
     */
    public static <S, D> void addConverter(Converter<S, D> converter) {
        MODEL_MAPPER.addConverter(converter);
    }

    /**
     * 添加映射
     *
     * @param propertyMap
     * @return
     */
    public <S, D> TypeMap<S, D> addMappings(PropertyMap<S, D> propertyMap) {
        return MODEL_MAPPER.addMappings(propertyMap);
    }
}
