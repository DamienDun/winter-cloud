package com.winter.cloud.common.web.handlers;

import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;


/**
 * 请求信息处理器
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2022/12/16 13:27
 */
public interface RequestMappingInfoHandler {

    /**
     * 映射处理
     *
     * @param mappingInfo   映射信息
     * @param handlerMethod 处理方法
     */
    void mappingHandler(RequestMappingInfo mappingInfo, HandlerMethod handlerMethod);
}
