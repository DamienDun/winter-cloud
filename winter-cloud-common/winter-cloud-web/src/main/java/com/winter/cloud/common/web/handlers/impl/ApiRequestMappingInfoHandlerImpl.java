package com.winter.cloud.common.web.handlers.impl;

import com.winter.cloud.common.core.utils.PackageUtils;
import com.winter.cloud.common.web.annotation.IgnoreApiResponseBody;
import com.winter.cloud.common.web.handlers.ApiRequestMappingInfoHandler;
import com.winter.cloud.common.web.util.UrlUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2022/12/16 14:05
 */
public class ApiRequestMappingInfoHandlerImpl implements ApiRequestMappingInfoHandler {

    protected final Set<String> apiControllerPackageSet;

    /**
     * value == true 时，表示忽略
     */
    private final Map<String, Boolean> API_URL_MAP = new ConcurrentHashMap<>(100);

    /**
     * 服务器属性
     */
    @Autowired
    protected ServerProperties serverProperties;

    /**
     * 实例化
     *
     * @param apiControllerPackages api控制器包集合
     */
    public ApiRequestMappingInfoHandlerImpl(String[] apiControllerPackages) {
        String[] pcks = PackageUtils.toPackages(apiControllerPackages);
        this.apiControllerPackageSet = new HashSet<>(pcks.length);
        this.apiControllerPackageSet.addAll(Arrays.asList(pcks));
    }

    @Override
    public void mappingHandler(RequestMappingInfo mappingInfo, HandlerMethod handlerMethod) {
        if (this.isContainPackage(handlerMethod)) {
            boolean ignoreFlag = isIgnore(handlerMethod);
            Set<RequestMethod> methods = mappingInfo.getMethodsCondition().getMethods();
            PatternsRequestCondition patternsCondition = mappingInfo.getPatternsCondition();
            Set<String> patterns = Objects.nonNull(patternsCondition) ? patternsCondition.getPatterns() : Collections.emptySet();
            for (String url : patterns) {
                String accessUrl = UrlUtils.getConfigureCaseLowerRequestUrl(this.serverProperties.getServlet().getContextPath(), url);
                for (RequestMethod method : methods) {
                    API_URL_MAP.put(accessUrl + ":" + method.toString().toLowerCase(), ignoreFlag);
                }
            }
        }
    }

    /**
     * 是否包含特定包
     *
     * @param handlerMethod 函数
     * @return
     */
    private boolean isContainPackage(HandlerMethod handlerMethod) {
        if (this.apiControllerPackageSet.size() == 0) {
            return true;
        }
        String name = handlerMethod.getBeanType().getPackage().getName();
        if (this.apiControllerPackageSet.contains(name)) {
            return true;
        }
        for (String pckName : this.apiControllerPackageSet) {
            if (name.startsWith(pckName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 是否忽略
     *
     * @param handlerMethod 函数处理器
     * @return
     */
    private boolean isIgnore(HandlerMethod handlerMethod) {
        IgnoreApiResponseBody ignoreBody = handlerMethod.getMethod().getAnnotation(IgnoreApiResponseBody.class);
        if (ignoreBody != null) {
            return true;
        }
        Class<?> type = handlerMethod.getBeanType();
        do {
            ignoreBody = type.getAnnotation(IgnoreApiResponseBody.class);
            if (ignoreBody != null) {
                return true;
            }
            type = type.getSuperclass();
        } while (!type.equals(Object.class));
        return false;
    }

    @Override
    public boolean isApiRequest(HttpServletRequest request) {
        return this.isApiRequest(request.getRequestURI(), request.getMethod());
    }

    @Override
    public boolean isApiRequest(String requestUrl, String requestMethod) {
        Boolean value = API_URL_MAP.get(UrlUtils.getLowerCaseRequestUrl(requestUrl) + ":" + requestMethod.toLowerCase());
        return value != null && !value;
    }
}
