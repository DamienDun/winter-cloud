package com.winter.cloud.common.web.util;

import com.winter.cloud.common.core.exception.ServiceException;
import com.winter.cloud.common.core.utils.StringUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2023/12/13 9:16
 */
public class ControllerUtils {

    /**
     * 解析参数
     *
     * @param id
     * @return
     */
    public static Long parseParameterById(String id) {
        if (StringUtils.isEmpty(id)) {
            throw new ServiceException("无效的路径。");
        }
        try {
            return Long.parseLong(id.trim());
        } catch (Exception err) {
            throw new ServiceException("参数[" + id + "]无效,格式不正确。");
        }
    }

    /**
     * Url编号
     *
     * @param value   值
     * @param charset 编码
     * @return
     */
    public static String urlEncode(String value, Charset charset) {
        if (value == null) {
            return "";
        }
        if (charset == null) {
            charset = StandardCharsets.UTF_8;
        }
        try {
            return URLEncoder.encode(value, charset.name());
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }

    /**
     * 设置响应文件名
     *
     * @param response 响应
     * @param fileName
     */
    public static void setResponseFileName(HttpServletResponse response, String fileName) {
        fileName = urlEncode(fileName, StandardCharsets.UTF_8);
        String headerKey = "Content-Disposition";
        String headerValue = String.format("attachment; filename=\"%s\"", fileName);
        response.setHeader(headerKey, headerValue);
    }
}
