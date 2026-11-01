package com.winter.cloud.common.filestorage.annotation;

import com.winter.cloud.common.filestorage.configure.WinterStorageAutoConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 启用存储
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2022/8/15 19:23
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import({WinterStorageAutoConfiguration.class})
public @interface EnableWinterStorage {
}
