package com.winter.cloud.common.core.utils;

import com.fasterxml.jackson.databind.BeanProperty;
import com.winter.cloud.common.core.utils.time.Time;
import com.winter.cloud.common.core.utils.time.TimeSpan;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 类型工具类
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2022/8/29 8:53
 */
public class TypeUtils {

    private static final Map<Class<?>, Integer> BASE_TYPE_MAP = new HashMap<>(50);
    private static final Map<Class<?>, Map<BeanProperty, String>> PROPERTY_FRIENDLY_MAP = new ConcurrentHashMap<>(300);

    static {
        /*
         * 0表示通用，数字>=1,1=表示整数,2=浮点数，11=长整数，12=长实数
         */
        BASE_TYPE_MAP.put(String.class, 0);
        BASE_TYPE_MAP.put(Time.class, 0);
        BASE_TYPE_MAP.put(TimeSpan.class, 0);
        BASE_TYPE_MAP.put(Date.class, 0);
        BASE_TYPE_MAP.put(LocalDate.class, 0);
        BASE_TYPE_MAP.put(LocalDateTime.class, 0);
        BASE_TYPE_MAP.put(LocalTime.class, 0);
        BASE_TYPE_MAP.put(OffsetDateTime.class, 0);
        BASE_TYPE_MAP.put(Byte.TYPE, 1);
        BASE_TYPE_MAP.put(Byte.class, 1);
        BASE_TYPE_MAP.put(Short.TYPE, 1);
        BASE_TYPE_MAP.put(Short.class, 1);
        BASE_TYPE_MAP.put(Character.TYPE, 0);
        BASE_TYPE_MAP.put(Character.class, 0);
        BASE_TYPE_MAP.put(Integer.TYPE, 1);
        BASE_TYPE_MAP.put(Integer.class, 1);
        BASE_TYPE_MAP.put(Long.TYPE, 1);
        BASE_TYPE_MAP.put(Long.class, 1);
        BASE_TYPE_MAP.put(Float.TYPE, 2);
        BASE_TYPE_MAP.put(Float.class, 2);
        BASE_TYPE_MAP.put(Double.TYPE, 2);
        BASE_TYPE_MAP.put(Double.class, 2);
        BASE_TYPE_MAP.put(Boolean.TYPE, 0);
        BASE_TYPE_MAP.put(Boolean.class, 0);
        BASE_TYPE_MAP.put(java.sql.Time.class, 0);
        BASE_TYPE_MAP.put(java.sql.Date.class, 0);
        BASE_TYPE_MAP.put(java.sql.Timestamp.class, 0);
        BASE_TYPE_MAP.put(BigInteger.class, 11);
        BASE_TYPE_MAP.put(BigDecimal.class, 12);
        BASE_TYPE_MAP.put(Enum.class, 0);
    }

    /**
     * 是否是基础类型
     *
     * @param type 类型
     * @return
     * @author
     */
    public static boolean isBaseType(Class<?> type) {
        return BASE_TYPE_MAP.containsKey(type) || Enum.class.isAssignableFrom(type);
    }

    /**
     * 是否为数字类型
     *
     * @param type 类型
     * @return byte、Byte、short、Short、int、Integer、long、Long、float、Float、double、Double、BigInteger、BigDecimal
     * 则为 true, 否则为false
     * @author
     */
    public static boolean isNumberType(Class<?> type) {
        return Number.class.isAssignableFrom(type);
    }

    /**
     * 是否为整数类型
     *
     * @param type 类型
     * @return byte、Byte、short、Short、int、Integer、long、Long 则为 true,否则为 false
     * @author
     */
    public static boolean isIntegerType(Class<?> type) {
        Integer value = BASE_TYPE_MAP.get(type);
        if (value == null) {
            return false;
        }
        return value == 1;
    }

    /**
     * 是否为浮点数类型
     *
     * @param type 类型
     * @return float、Float、double、Double 为 true，否则为 false
     * @author
     */
    public static boolean isFloatType(Class<?> type) {
        Integer value = BASE_TYPE_MAP.get(type);
        if (value == null) {
            return false;
        }
        return value == 2;
    }

    /**
     * 是否是二进制类型
     *
     * @param type 类型
     * @return
     * @author
     */
    public static boolean isBinaryType(Class<?> type) {
        if (type == null || !type.isArray()) {
            return false;
        }
        return type.equals(byte[].class) || type.equals(Byte[].class);
    }

    /**
     * 是否是基础或二进制类型
     *
     * @param type 类型
     * @return
     * @author
     */
    public static boolean isBaseOrBinaryType(Class<?> type) {
        return isBaseType(type) || isBinaryType(type);
    }

    /**
     * 判断类是否包含特定的注解
     *
     * @param fullClassName
     * @param annotationClass
     * @return
     * @throws ClassNotFoundException
     */
    public static boolean isAnnotationClass(String fullClassName, Class<? extends Annotation> annotationClass)
            throws ClassNotFoundException {
        if (fullClassName == null || fullClassName.trim().length() == 0) {
            throw new NullPointerException("fullClassName 为 null 或空字符串");
        }
        return isAnnotationClass(Class.forName(fullClassName), annotationClass);
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
     * 是否是接口类型
     *
     * @param type 类型
     * @return
     */
    public static boolean isInterface(Class<?> type) {
        int mod = type.getModifiers();
        return Modifier.isInterface(mod);
    }

    /**
     * 是否是抽象类型
     *
     * @param type 类型
     * @return
     */
    public static boolean isAbstract(Class<?> type) {
        int mod = type.getModifiers();
        return Modifier.isAbstract(mod);
    }

    /**
     * 是否是公用类型
     *
     * @param type 类型
     * @return
     */
    public static boolean isPublic(Class<?> type) {
        int mod = type.getModifiers();
        return Modifier.isPublic(mod);
    }

    /**
     * 是否拥有公用的默认构造
     *
     * @param type 类型
     * @return
     */
    public static boolean isDefaultPublicConstructor(Class<?> type) {
        Constructor<?>[] conArray;
        try {
            conArray = type.getConstructors();
            for (Constructor<?> constructor : conArray) {
                if (constructor.getParameterTypes().length == 0) {
                    return true;
                }
            }
            return false;
        } catch (SecurityException e) {
            return false;
        }
    }

    /**
     * 是否是私有类型
     *
     * @param type 类型
     * @return
     */
    public static boolean isPrivate(Class<?> type) {
        int mod = type.getModifiers();
        return Modifier.isPrivate(mod);
    }

    /**
     * 是否是 protected 类型
     *
     * @param type 类型
     * @return
     */
    public static boolean isProtected(Class<?> type) {
        int mod = type.getModifiers();
        return Modifier.isProtected(mod);
    }

    /**
     * 是否是 final 类型(不可继承)
     *
     * @param type 类型
     * @return
     */
    public static boolean isFinal(Class<?> type) {
        int mod = type.getModifiers();
        return Modifier.isFinal(mod);
    }

    /**
     * 是否是静态类型
     *
     * @param type 类型
     * @return
     */
    public static boolean isStatic(Class<?> type) {
        int mod = type.getModifiers();
        return Modifier.isStatic(mod);
    }

    /**
     * 加载类名称
     *
     * @param className 类名称
     * @return 不存在则返回 null
     */
    public static Class<?> forName(String className) {
        ExceptionUtil.checkNotNull(className, "className");
        try {
            return Class.forName(className);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 是否存在类名称
     *
     * @param className 类名称类名称
     * @return
     */
    public static boolean isExistClassName(String className) {
        return forName(className) != null;
    }

    /**
     * 获取所有接口
     *
     * @param type
     * @return
     */
    public static Set<Class<?>> getInterfaces(Class<?> type) {
        ExceptionUtil.checkNotNull(type, "type");
        Set<Class<?>> types = new HashSet<>();
        Class<?>[] interfaces = type.getInterfaces();
        for (Class<?> interfaceType : interfaces) {
            types.add(interfaceType);
            types.addAll(getInterfaces(interfaceType));
        }
        return types;
    }

    /**
     * 比较两个类型是否完成相同
     *
     * @param leftTypes  右边类型集合
     * @param rigthTypes 右边类型集合
     * @return
     */
    public static boolean equalsTypes(Class<?>[] leftTypes, Class<?>[] rigthTypes) {
        if (leftTypes == null) {
            leftTypes = new Class<?>[0];
        }
        if (rigthTypes == null) {
            rigthTypes = new Class<?>[0];
        }
        if (leftTypes.length != rigthTypes.length) {
            return false;
        }
        for (int i = 0; i < leftTypes.length; i++) {
            if (!leftTypes[i].equals(rigthTypes[i])) {
                return false;
            }
        }
        return true;
    }

}
