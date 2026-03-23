package com.winter.cloud.common.core.exception.argument;

/**
 * 参数为null引发的异常
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2022/8/15 18:30
 */
public class ArgumentNullException extends ArgumentException {

    private static final long serialVersionUID = -6122218478853155619L;

    public ArgumentNullException(String message, Object... args) {
        super("argument.null", message, args);
    }
}
