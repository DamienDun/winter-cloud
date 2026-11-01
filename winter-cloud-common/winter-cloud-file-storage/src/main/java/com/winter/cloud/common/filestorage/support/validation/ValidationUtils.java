package com.winter.cloud.common.filestorage.support.validation;

import com.winter.cloud.common.filestorage.support.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Configuration;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Set;

/** 校验工具 */
public final class ValidationUtils {

    private static final Logger logger = LoggerFactory.getLogger(ValidationUtils.class);
    private static final Validator VALIDATOR;

    static {
        Validator validator;
        try {
            Configuration<?> config = Validation.byDefaultProvider().configure();
            ValidatorFactory factory = config.buildValidatorFactory();
            validator = factory.getValidator();
        } catch (Exception err) {
            validator = null;
            logger.error(err.getMessage(), err);
        }
        VALIDATOR = validator;
    }

    private ValidationUtils() {
    }

    public static void validation(final Object obj) {
        ExceptionUtils.checkNotNull(obj, "obj");
        if (VALIDATOR == null) {
            logger.error("初始化 ValidatorFactory 失败，无法调用验证。");
            return;
        }
        Set<ConstraintViolation<Object>> violations = VALIDATOR.validate(obj);
        if (!violations.isEmpty()) {
            violations.forEach(s -> ExceptionUtils.throwValidException(s.getMessage()));
        }
    }
}