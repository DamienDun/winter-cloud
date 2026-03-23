package com.winter.cloud.common.core.utils.bean;


import com.winter.cloud.common.core.utils.TypeUtils;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bean 工具类
 *
 * @author winter
 */
public class BeanUtils extends org.springframework.beans.BeanUtils {
    /**
     * Bean方法名中属性名开始的下标
     */
    private static final int BEAN_METHOD_PROP_INDEX = 3;

    /**
     * 匹配getter方法的正则表达式
     */
    private static final Pattern GET_PATTERN = Pattern.compile("get(\\p{javaUpperCase}\\w*)");

    /**
     * 匹配setter方法的正则表达式
     */
    private static final Pattern SET_PATTERN = Pattern.compile("set(\\p{javaUpperCase}\\w*)");

    /**
     * Bean属性复制工具方法。
     *
     * @param dest 目标对象
     * @param src  源对象
     */
    public static void copyBeanProp(Object dest, Object src) {
        try {
            copyProperties(src, dest);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取对象的setter方法。
     *
     * @param obj 对象
     * @return 对象的setter方法列表
     */
    public static List<Method> getSetterMethods(Object obj) {
        // setter方法列表
        List<Method> setterMethods = new ArrayList<Method>();

        // 获取所有方法
        Method[] methods = obj.getClass().getMethods();

        // 查找setter方法

        for (Method method : methods) {
            Matcher m = SET_PATTERN.matcher(method.getName());
            if (m.matches() && (method.getParameterTypes().length == 1)) {
                setterMethods.add(method);
            }
        }
        // 返回setter方法列表
        return setterMethods;
    }

    /**
     * 获取对象的getter方法。
     *
     * @param obj 对象
     * @return 对象的getter方法列表
     */

    public static List<Method> getGetterMethods(Object obj) {
        // getter方法列表
        List<Method> getterMethods = new ArrayList<Method>();
        // 获取所有方法
        Method[] methods = obj.getClass().getMethods();
        // 查找getter方法
        for (Method method : methods) {
            Matcher m = GET_PATTERN.matcher(method.getName());
            if (m.matches() && (method.getParameterTypes().length == 0)) {
                getterMethods.add(method);
            }
        }
        // 返回getter方法列表
        return getterMethods;
    }

    /**
     * 检查Bean方法名中的属性名是否相等。<br>
     * 如getName()和setName()属性名一样，getName()和setAge()属性名不一样。
     *
     * @param m1 方法名1
     * @param m2 方法名2
     * @return 属性名一样返回true，否则返回false
     */

    public static boolean isMethodPropEquals(String m1, String m2) {
        return m1.substring(BEAN_METHOD_PROP_INDEX).equals(m2.substring(BEAN_METHOD_PROP_INDEX));
    }

    /**
     * 对象分解处理
     * <see>
     * 将一个对象进行分解，如果对象为基本类型、集合、Map、数组等类型则会自动分解，消费器只对类对象进行处理
     * </see>
     *
     * @param obj
     * @param objectConsumer
     */
    public static void objectResolveHandle(Object obj, Consumer<Object> objectConsumer) {
        if (obj == null || objectConsumer == null) {
            return;
        }
        final Set<Object> objSet = new HashSet<>(16);
        objectResolveHandle(objSet, obj, objectConsumer);
    }

    private static void objectResolveHandle(Set<Object> objSet, Object obj, Consumer<Object> objectConsumer) {
        if (obj instanceof Collection) {
            Collection items = (Collection) obj;
            for (Object value : items) {
                if (value != null) {
                    objectResolveHandle(objSet, value, objectConsumer);
                }
            }
        } else if (obj instanceof Map) {
            Map map = (Map) obj;
            Set<Map.Entry> entrySet = map.entrySet();
            Iterator<Map.Entry> entryIterator = entrySet.iterator();
            while (entryIterator.hasNext()) {
                Map.Entry next = entryIterator.next();
                Object key = next.getKey();
                Object value = next.getValue();
                if (key != null) {
                    objectResolveHandle(objSet, key, objectConsumer);
                }
                if (value != null) {
                    objectResolveHandle(objSet, value, objectConsumer);
                }
            }
        } else {
            Class<?> type = obj.getClass();
            if (TypeUtils.isBaseOrBinaryType(type)) {
                return;
            }
            if (type.isArray()) {
                int length = Array.getLength(obj);
                for (int i = 0; i < length; i++) {
                    Object value = Array.get(obj, i);
                    if (value != null) {
                        objectResolveHandle(objSet, value, objectConsumer);
                    }
                }
            } else {
                // 防无限递归
                if (objSet.add(obj)) {
                    objectConsumer.accept(obj);
                }
            }
        }
    }
}
