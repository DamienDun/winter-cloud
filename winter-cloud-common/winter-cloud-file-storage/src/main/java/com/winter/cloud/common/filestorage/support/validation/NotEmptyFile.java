package com.winter.cloud.common.filestorage.support.validation;

import org.springframework.web.multipart.MultipartFile;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Objects;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/** 上传文件非空校验 */
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER})
@Retention(RUNTIME)
@Documented
@Constraint(validatedBy = NotEmptyFile.ValidChecker.class)
public @interface NotEmptyFile {

    String message() default "文件不能为空";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    class ValidChecker implements ConstraintValidator<NotEmptyFile, MultipartFile> {
        private String message;

        @Override
        public void initialize(NotEmptyFile constraintAnnotation) {
            message = constraintAnnotation.message();
        }

        @Override
        public boolean isValid(MultipartFile file, ConstraintValidatorContext context) {
            context.buildConstraintViolationWithTemplate(message);
            return Objects.nonNull(file) && !file.isEmpty() && file.getSize() > 0;
        }
    }
}