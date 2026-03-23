package com.winter.cloud.common.core.utils;

import com.winter.cloud.common.core.exception.argument.ArgumentBlankException;
import com.winter.cloud.common.core.exception.argument.ArgumentNullException;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * 错误信息处理类。
 *
 * @author winter
 */
public class ExceptionUtil {
    /**
     * 获取exception的详细错误信息。
     */
    public static String getExceptionMessage(Throwable e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw, true));
        return sw.toString();
    }

    public static String getRootErrorMessage(Exception e) {
        Throwable root = ExceptionUtils.getRootCause(e);
        root = (root == null ? e : root);
        if (root == null) {
            return "";
        }
        String msg = root.getMessage();
        if (msg == null) {
            return "null";
        }
        return StringUtils.defaultString(msg);
    }

    /**
     * 检查非空
     *
     * @param value 值
     * @return
     */
    public static <T> T checkNotNull(T value) {
        if (value == null) {
            throw new ArgumentNullException("参数值为 null。");
        }
        return value;
    }

    /**
     * 检查非空
     *
     * @param value     值
     * @param paramName 参数名
     * @return
     */
    public static <T> T checkNotNull(T value, String paramName) {
        if (value == null) {
            throw new ArgumentNullException(String.format("参数 %s 的值为 null。", paramName));
        }
        return value;
    }

    /**
     * 检查非空和非空白
     *
     * @param value 值
     * @return
     */
    public static String checkNotNullOrBlank(String value) {
        checkNotNull(value);
        if (StringUtils.isEmpty(value)) {
            throw new ArgumentBlankException("参数值为空白值。");
        }
        return value;
    }

    /**
     * 检查非空和非空白
     *
     * @param value 值
     * @return
     * @author 老码农 2017-10-09 16:57:30
     */
    public static String checkNotNullOrBlank(String value, String paramName) {
        checkNotNull(value, paramName);
        if (StringUtils.isEmpty(value)) {
            throw new ArgumentBlankException(String.format("参数 %s 的值为空白值。", paramName), paramName);
        }
        return value;
    }

}
