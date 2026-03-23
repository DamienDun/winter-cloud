package com.winter.cloud.common.swagger.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Api 头请求信息
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2022/8/17 15:38
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({})
@Documented
public @interface ApiHeaderParameter {

	/**
	 * 字段 name
	 */
    String FIELD_NAME = "name";

	/**
	 * 字段 dataType
	 */
    String FIELD_DATA_TYPE = "dataType";

	/**
	 * 字段 description
	 */
    String FIELD_DESCRIPTION = "description";

	/**
	 * 字段 required
	 */
    String FIELD_REQUIRED = "required";

	/**
	 * 参数名称
	 *
	 * @return
	 *
	 */
	String name();

	/**
	 * 类型
	 *
	 * @return
	 *
	 */
	String dataType() default "string";

	/**
	 * 说明
	 *
	 * @return
	 *
	 */
	String description() default "";

	/**
	 * 是否必须
	 *
	 * @return
	 *
	 */
	boolean required() default false;
}
