package com.winter.cloud.common.filestorage.support.exception;

import com.winter.cloud.common.core.exception.WinterError;
import com.winter.cloud.common.core.exception.base.BaseException;

/** 校验异常 */
public class ValidException extends BaseException {

    private static final long serialVersionUID = 1L;

    public ValidException(String defaultMessage) {
        super("valid", String.valueOf(WinterError.SystemErrorCode.VALIDATION_ERRORCODE), null, defaultMessage);
    }

    public ValidException(Throwable cause, String defaultMessage) {
        super(cause, defaultMessage);
    }
}