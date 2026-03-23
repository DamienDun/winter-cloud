package com.winter.cloud.common.core.exception;

/**
 * 异常级别
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2023/12/13 13:33
 */
public enum ErrorLevel {

    /**
     * 未知
     */
    NONE(0),
    /**
     * 用户级别
     */
    USER(1),
    /**
     * 应用级别
     */
    APPLICATION(2),
    /**
     * 系统级别
     */
    SYSTEM(3);

    private final int code;

    /**
     * 实例化
     *
     * @param code
     */
    private ErrorLevel(int code) {
        this.code = code;
    }

    /**
     * 获取代码
     *
     * @return
     * @author
     */
    public int getCode() {
        return code;
    }
}
