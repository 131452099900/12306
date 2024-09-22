package me.xgwd.web.advise;

import me.xgwd.api.trace.TraceUtil;
import me.xgwd.base.resp.Result;
import org.slf4j.MDC;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 给web接口返回结果设置traceID
 */
@RestControllerAdvice(basePackages = "me.xgwd")
public class WebResponseModifyAdvice implements ResponseBodyAdvice {

    @Override
    public boolean supports(final MethodParameter methodParameter, final Class converterType) {
        // 返回 class 为 ApiResult（带 TraceId 属性） & converterType 为 Json 转换
        return methodParameter.getMethod().getReturnType().isAssignableFrom(Result.class)
                && converterType.isAssignableFrom(MappingJackson2HttpMessageConverter.class);
    }

    @Override
    public Object beforeBodyWrite(final Object body, final MethodParameter methodParameter, final MediaType mediaType, final Class aClass,
                                  final ServerHttpRequest serverHttpRequest, final ServerHttpResponse serverHttpResponse) {
        // 设置 TraceId
        ((Result<?>) body).setTraceId(MDC.get(TraceUtil.TRACE_ID));
        return body;
    }
}
