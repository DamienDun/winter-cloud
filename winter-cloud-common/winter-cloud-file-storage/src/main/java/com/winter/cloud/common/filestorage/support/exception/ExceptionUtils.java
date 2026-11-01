package com.winter.cloud.common.filestorage.support.exception;

import com.winter.cloud.common.core.exception.argument.ArgumentNullException;

/** 异常工具 */
public final class ExceptionUtils {

    private ExceptionUtils() {
    }

    public static <T> T checkNotNull(T value, String paramName) {
        if (value == null) {
            throw new ArgumentNullException(String.format("参数 %s 的值为 null。", paramName));
        }
        return value;
    }

    public static ValidException throwValidException(String message) {
        return throwValidException(message, null);
    }

    public static ValidException throwValidException(String message, Throwable cause) {
        throw new ValidException(cause, message);
    }
}