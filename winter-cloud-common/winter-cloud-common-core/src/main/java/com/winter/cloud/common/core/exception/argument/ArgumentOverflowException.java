package com.winter.cloud.common.core.exception.argument;

/**
 * 参数超出范围引发的异常
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2022/8/15 18:36
 */
public class ArgumentOverflowException extends ArgumentException {

    private static final long serialVersionUID = -8079029178273725004L;

    public ArgumentOverflowException(String message, Object... args) {
        super("argument.overflow", message, args);
    }
}
