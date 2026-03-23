package com.winter.cloud.common.core.exception;

import com.winter.cloud.common.core.exception.base.BaseException;

/**
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2023/12/13 13:28
 */
public class FormatException extends BaseException {

    private static final long serialVersionUID = -3726469063074882283L;

    /**
     * 无构造实例化
     */
    public FormatException() {
        this("格式不正确");
    }

    public FormatException(String defaultMessage) {
        super("format", String.valueOf(WinterError.SystemErrorCode.FORMAT_ERRORCODE), null, defaultMessage);
    }

    public FormatException(Throwable cause, String defaultMessage) {
        super(cause, defaultMessage);
    }
}
