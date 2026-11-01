package com.winter.cloud.common.filestorage.support.validation;

import com.winter.cloud.common.core.utils.StringUtils;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/** 非空字符串校验 */
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER})
@Retention(RUNTIME)
@Documented
@Constraint(validatedBy = NotNullOrBlank.ValidChecker.class)
public @interface NotNullOrBlank {

    String message() default "不能为空";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    class ValidChecker implements ConstraintValidator<NotNullOrBlank, String> {
        private String message;

        @Override
        public void initialize(NotNullOrBlank constraintAnnotation) {
            message = constraintAnnotation.message();
        }

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            context.buildConstraintViolationWithTemplate(message);
            return !StringUtils.isEmpty(value);
        }
    }
}