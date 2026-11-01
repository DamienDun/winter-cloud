package com.winter.cloud.common.filestorage.support.exception;

import com.winter.cloud.common.core.exception.WinterError;
import com.winter.cloud.common.core.exception.base.BaseException;

/** 不支持异常 */
public class NotSupportException extends BaseException {

    private static final long serialVersionUID = 1L;

    public NotSupportException(String defaultMessage) {
        super("notsupport", String.valueOf(WinterError.SystemErrorCode.NOT_SUPPORT_ERRORCODE), null, defaultMessage);
    }

    public NotSupportException(Throwable cause, String defaultMessage) {
        super(cause, defaultMessage);
    }
}