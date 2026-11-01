package com.winter.cloud.common.web.handlers;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.winter.cloud.common.core.constant.Constants;
import com.winter.cloud.common.core.web.domain.AjaxResult;
import com.winter.cloud.common.core.domain.R;
import com.winter.cloud.common.core.domain.Rpage;
import com.winter.cloud.common.core.web.page.TableDataInfo;
import com.winter.cloud.common.core.exception.ServiceException;
import com.winter.cloud.common.core.utils.DateUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import javax.servlet.http.HttpServletRequest;

/**
 * 全局接口响应体处理
 * <p>
 * </p>
 *
 * @author Damien
 * @description
 * @create 2022/12/16 10:06
 */
@ConditionalOnBean(ApiRequestMappingInfoHandler.class)
@RestControllerAdvice
public class ApiResponseBodyHandler implements ResponseBodyAdvice<Object> {

    protected final ApiRequestMappingInfoHandler apiRequestMappingInfoHandler;

    public ApiResponseBodyHandler(ApiRequestMappingInfoHandler apiRequestMappingInfoHandler) {
        this.apiRequestMappingInfoHandler = apiRequestMappingInfoHandler;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {
        if (!response.getHeaders().containsKey(Constants.RESP_HEADER_CODE)) {
            response.getHeaders().add(Constants.RESP_ACCESS_CONTROL_EXPOSE_HEADERS, Constants.RESP_HEADER_CODE);
            response.getHeaders().add(Constants.RESP_HEADER_CODE, String.valueOf(HttpStatus.OK.value()));
        }
        if (!response.getHeaders().containsKey(Constants.RESP_HEADER_DATE)) {
            response.getHeaders().add(Constants.RESP_ACCESS_CONTROL_EXPOSE_HEADERS, Constants.RESP_HEADER_DATE);
            response.getHeaders().add(Constants.RESP_HEADER_DATE, DateUtils.getTime());
        }
        boolean isApi;
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest httpRequest = (ServletServerHttpRequest) request;
            isApi = this.apiRequestMappingInfoHandler.isApiRequest(httpRequest.getServletRequest());
        } else {
            isApi = this.apiRequestMappingInfoHandler.isApiRequest((HttpServletRequest) request);
        }
        if (!isApi) {
            return body;
        }
        if (body instanceof R) {
            return body;
        }
        if (body instanceof Rpage) {
            return body;
        }
        if (body instanceof AjaxResult) {
            return body;
        }
        if (body instanceof TableDataInfo) {
            return body;
        }
        // String类型不能直接包装，所以要进行些特别的处理
        if (body instanceof String) {
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                return objectMapper.writeValueAsString(R.ok(body));
            } catch (JsonProcessingException e) {
                throw new ServiceException("返回String类型错误");
            }
        }
        if (body instanceof IPage) {
            return Rpage.ok((IPage) body);
        }
        return R.ok(body);
    }
}
