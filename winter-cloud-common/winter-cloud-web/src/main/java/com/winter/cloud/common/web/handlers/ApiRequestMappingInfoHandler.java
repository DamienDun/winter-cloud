package com.winter.cloud.common.web.handlers;

import javax.servlet.http.HttpServletRequest;

/**
 * Api请求处理器
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2022/12/16 14:04
 */
public interface ApiRequestMappingInfoHandler extends RequestMappingInfoHandler {

    /**
     * 是否为 Api 请求
     *
     * @param request 请求
     * @return
     */
    boolean isApiRequest(HttpServletRequest request);

    /**
     * 是否为 Api 请求
     *
     * @param requestUrl    请求地址
     * @param requestMethod 请求方法
     * @return
     */
    boolean isApiRequest(String requestUrl, String requestMethod);
}
