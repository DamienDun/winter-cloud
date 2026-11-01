package com.winter.cloud.common.swagger.config;

import com.winter.cloud.common.swagger.models.WinterSwaggerApiGroupInfo;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.GroupedOpenApi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 把 {@link WinterSwaggerApiGroupInfo} 转成 SpringDoc 分组。
 * <p>
 * SpringDoc 1.7 要求分组至少有包、路径或方法过滤器，否则 build 直接抛异常。
 * 这里按注解优先、包次之、都没有则只收 {@link Tag} 的顺序补上过滤条件。
 * 不强制方法上有 {@code @Operation}：多数控制器还没标这个注解，加上去文档会被清空。
 * </p>
 *
 * @author winter
 */
public final class WinterSwaggerGroupFactory {

    private WinterSwaggerGroupFactory() {
    }

    public static GroupedOpenApi create(WinterSwaggerApiGroupInfo group) {
        GroupedOpenApi.Builder builder = GroupedOpenApi.builder().group(group.getGroupName());
        Class<? extends Annotation> annotation = group.getAnnotation();
        if (annotation != null) {
            // 指定了类注解就不再使用包路径，避免两种条件叠成交集后把接口滤没
            builder.addOpenApiMethodFilter(method -> hasClassAnnotation(method, annotation));
        } else {
            List<String> packages = readablePackages(group.getPackages());
            if (!packages.isEmpty()) {
                builder.packagesToScan(packages.toArray(new String[packages.size()]));
            } else {
                builder.addOpenApiMethodFilter(method -> hasClassAnnotation(method, Tag.class));
            }
        }
        return builder.build();
    }

    private static boolean hasClassAnnotation(Method method, Class<? extends Annotation> annotation) {
        return method.getDeclaringClass().isAnnotationPresent(annotation);
    }

    private static List<String> readablePackages(List<String> packages) {
        List<String> result = new ArrayList<String>();
        if (packages == null) {
            return result;
        }
        for (String packageName : packages) {
            if (packageName != null && packageName.trim().length() > 0) {
                result.add(packageName.trim());
            }
        }
        return result;
    }
}
