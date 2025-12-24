package com.tchalanet.server.common.web.advice;

import org.springframework.http.ProblemDetail;
import com.tchalanet.server.common.web.api.ApiNotice;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.api.ApiStatus;
import com.tchalanet.server.common.web.api.ServiceStatus;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.List;

/**
 * ResponseBodyAdvice that automatically wraps successful responses in ApiResponse.
 * Only wraps 2xx responses, leaves errors (ProblemDetail) unchanged.
 */
@RestControllerAdvice
public class ApiResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // Don't wrap if already ApiResponse or ProblemDetail
        return !ApiResponse.class.isAssignableFrom(returnType.getParameterType().getRawClass()) &&
               !ProblemDetail.class.isAssignableFrom(returnType.getParameterType().getRawClass());
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {

        ApiResponseContext context = ApiResponseContext.get();
        List<ApiNotice> notices = context.getNotices();
        List<ServiceStatus> services = context.getServices();

        // Determine status
        ApiStatus status;
        if (body == null && notices.stream().anyMatch(n -> "APPROVAL_REQUIRED".equals(n.code()))) {
            status = ApiStatus.PENDING;
        } else if (context.hasDegradedServices()) {
            status = ApiStatus.PARTIAL;
        } else if (context.hasWarnings()) {
            status = ApiStatus.SUCCESS_WITH_WARNINGS;
        } else {
            status = ApiStatus.SUCCESS;
        }

        return new ApiResponse<>(status, body, notices, services);
    }
}
