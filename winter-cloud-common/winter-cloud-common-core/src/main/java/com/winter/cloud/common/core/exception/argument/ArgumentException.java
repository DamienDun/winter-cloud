package com.winter.cloud.common.core.exception.argument;

import com.winter.cloud.common.core.exception.base.BaseException;

/**
 * 由于参数引发的异常
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2022/8/15 18:22
 */
public class ArgumentException extends BaseException {

    private static final long serialVersionUID = -2467146507347359882L;

    public ArgumentException() {
    }

    public ArgumentException(String code, String message, Object... args) {
        super(null, code, args, message);
    }
}
