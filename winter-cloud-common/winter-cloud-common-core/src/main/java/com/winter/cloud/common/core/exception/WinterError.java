package com.winter.cloud.common.core.exception;

/**
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2023/12/13 13:32
 */
public interface WinterError {

    /**
     *  应用异常代码
     *
     */
    public static class ApplicationErrorCode {

        /**
         * 应用异常默认代码
         */
        public static final int APPLICATION_ERRORCODE = - 30000;

        /**
         * 由于未登录而产生的异常代码
         */
        public static final int NOT_LOGIN_ERRORCODE = -1;

        /**
         * 由于权限引发的异常代码
         */
        public static final int AUTHORIZATION_ERRORCODE = -401;

        /**
         * 无效账户
         */
        public static final int ACCOUNT_INVALID_ERRORCODE = -101;

        /**
         * 由于账户状态异常代码
         */
        public static final int ACCOUNT_STATUS_ERRORCODE = -102;

        /**
         * 账户认证异常
         */
        public static final int ACCOUNT_CREDENTIALS_ERRORCODE = -103;

        /**
         * 票据无效
         */
        public static final int TOKEN_INVALID_ERRORCODE = -104;

    }

    /**
     * 系统异常代码
     *
     */
    public static class SystemErrorCode {
        /**
         * 系统异常默认代码
         */
        public static final int SYSTEM_ERRORCODE = -10000;

        /**
         * 由于不支持产生的异常代码
         */
        public static final int NOT_SUPPORT_ERRORCODE = SYSTEM_ERRORCODE - 1;

        /**
         * 由于格式出错产生的异常
         */
        public static final int FORMAT_ERRORCODE = SYSTEM_ERRORCODE - 2;

        /**
         * 由于签名产生的异常
         */
        public static final int SIGN_ERRORCODE = SYSTEM_ERRORCODE - 50;

        /**
         * 无效转换产生的异常代码
         */
        public static final int INVALIDCAST_ERRORCODE = SYSTEM_ERRORCODE - 2001;

        /**
         * 由于参数不正确引发的异常
         */
        public static final int ARGUMENT_ERRORCODE = SYSTEM_ERRORCODE - 1000;

        /**
         * 由于null参数引发的异常
         */
        public static final int ARGUMENT_NULL_ERRORCODE = ARGUMENT_ERRORCODE - 1;

        /**
         * 由于参数为blank引发的异常
         */
        public static final int ARGUMENT_BLANK_ERRORCODE = ARGUMENT_ERRORCODE - 2;

        /**
         * 由于参数超过范围引发的异常
         */
        public static final int ARGUMENT_OVERFLOW_ERRORCODE = ARGUMENT_ERRORCODE - 3;

        /**
         * 验证产生引发的异常代码
         */
        public static final int VALIDATION_ERRORCODE = SYSTEM_ERRORCODE - 2000;

        /**
         * 由于配置引发的异常
         */
        public static final int CONFIGURE_ERRORCODE = SYSTEM_ERRORCODE - 3000;

        /**
         * 由于数据库引发的异常
         */
        public static final int DB_BASE_ERRORCODE = SYSTEM_ERRORCODE - 4000;

        /**
         * 由于网络引发的异常
         */
        public static final int NETWORK_ERRORCODE = SYSTEM_ERRORCODE - 5000;
    }

    /**
     * 获取错误代码
     *
     * @return
     * @author
     */
    Integer getCode();

    /**
     * 设置错误代码
     *
     * @param code
     * @author
     */
    void setCode(Integer code);

    /**
     * 获取级别
     *
     * @return
     * @author
     */
    ErrorLevel getLevel();

    /**
     * 设置级别
     *
     * @param level
     * @author
     */
    void setLevel(ErrorLevel level);

    /**
     * 获取消息
     *
     * @return
     */
    String getMessage();
}
