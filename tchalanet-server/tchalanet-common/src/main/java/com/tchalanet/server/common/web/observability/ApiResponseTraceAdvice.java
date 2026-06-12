package com.tchalanet.server.common.web.observability;

import com.tchalanet.server.common.observability.TchTraceIds;
import com.tchalanet.server.common.web.api.ApiNotice;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.api.ApiStatus;
import com.tchalanet.server.common.web.api.ApiTraceInfo;
import com.tchalanet.server.common.web.api.ServiceStatus;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.List;

import static com.tchalanet.server.common.web.observability.RequiredRequestIdFilter.ATTR_REQUEST_ID;

/**
 * Injects a {@code trace} block into every successful {@link ApiResponse} JSON body.
 *
 * <p>Does not modify the {@link ApiResponse} record. Returns a thin projection
 * ({@link ApiResponseWithTrace}) that adds the trace field while preserving all
 * existing fields. Fail-open: absent trace context produces no {@code trace} field.
 *
 * <p>Runs after {@code ApiResponseBodyAdvice} (which wraps raw controller return values
 * into {@link ApiResponse}). The body seen here is always already an {@link ApiResponse}
 * or a non-ApiResponse type that is passed through unchanged.
 */
@RestControllerAdvice(basePackages = "com.tchalanet.server")
@Order(200)
public class ApiResponseTraceAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(
        @Nonnull MethodParameter returnType,
        @Nonnull Class<? extends HttpMessageConverter<?>> converterType
    ) {
        if (StringHttpMessageConverter.class.isAssignableFrom(converterType)) return false;
        if (ByteArrayHttpMessageConverter.class.isAssignableFrom(converterType)) return false;
        if (ResourceHttpMessageConverter.class.isAssignableFrom(converterType)) return false;
        return true;
    }

    @Override
    public Object beforeBodyWrite(
        @Nullable Object body,
        @Nonnull MethodParameter returnType,
        @Nonnull MediaType selectedContentType,
        @Nonnull Class<? extends HttpMessageConverter<?>> selectedConverterType,
        @Nonnull ServerHttpRequest request,
        @Nonnull ServerHttpResponse response
    ) {
        if (!(body instanceof ApiResponse<?> apiResponse)) {
            return body;
        }

        var trace = buildTrace(request);
        if (trace == null) {
            return body;
        }

        return new ApiResponseWithTrace<>(
            apiResponse.status(),
            apiResponse.data(),
            apiResponse.notices(),
            apiResponse.services(),
            trace
        );
    }

    @Nullable
    private static ApiTraceInfo buildTrace(ServerHttpRequest request) {
        String requestId = null;
        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpServletRequest httpReq = servletRequest.getServletRequest();
            requestId = (String) httpReq.getAttribute(ATTR_REQUEST_ID);
        }
        var traceId = TchTraceIds.currentTraceId();
        var spanId  = TchTraceIds.currentSpanId();

        var trace = ApiTraceInfo.of(requestId, traceId, spanId);
        return trace.hasAny() ? trace : null;
    }

    /**
     * Thin projection of {@link ApiResponse} that adds a {@code trace} field.
     * Serialized by Jackson as a plain object — no record modification needed.
     */
    public record ApiResponseWithTrace<T>(
        ApiStatus status,
        T data,
        List<ApiNotice> notices,
        List<ServiceStatus> services,
        ApiTraceInfo trace
    ) {}
}
