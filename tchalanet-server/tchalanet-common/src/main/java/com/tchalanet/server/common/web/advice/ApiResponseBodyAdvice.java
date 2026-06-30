package com.tchalanet.server.common.web.advice;

import com.tchalanet.server.common.web.api.ApiNotice;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.api.ApiStatus;
import com.tchalanet.server.common.web.api.NoticeSeverity;
import com.tchalanet.server.common.web.api.ServiceHealth;
import com.tchalanet.server.common.web.api.ServiceStatus;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * ResponseBodyAdvice that automatically wraps successful responses in ApiResponse.
 *
 * <p>Status decision tree (in order):
 * <ol>
 *   <li>Any notice with code {@code APPROVAL_REQUIRED} → {@code PENDING},
 *       regardless of whether a body is present. The caller will display the
 *       body (e.g. the placed ticket awaiting approval) along with a pending
 *       banner.</li>
 *   <li>Any degraded service → {@code PARTIAL}.</li>
 *   <li>Any WARN notice → {@code SUCCESS_WITH_WARNINGS}.</li>
 *   <li>Otherwise → {@code SUCCESS}.</li>
 * </ol>
 */
@RestControllerAdvice(basePackages = "com.tchalanet.server")
@Order(100)
public class ApiResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    public static final String APPROVAL_REQUIRED_CODE = "APPROVAL_REQUIRED";

    @Override
    public boolean supports(
        @Nonnull MethodParameter returnType,
        @Nonnull Class<? extends HttpMessageConverter<?>> converterType) {

        if (StringHttpMessageConverter.class.isAssignableFrom(converterType)) return false;
        if (ByteArrayHttpMessageConverter.class.isAssignableFrom(converterType)) return false;
        if (ResourceHttpMessageConverter.class.isAssignableFrom(converterType)) return false;

        Class<?> paramType = returnType.getParameterType();

        if (ResponseEntity.class.isAssignableFrom(paramType)) {
            Type gen = returnType.getGenericParameterType();
            if (gen instanceof ParameterizedType pt) {
                var cls = rawClass(pt.getActualTypeArguments()[0]);
                if (cls == null) {
                    return false;
                }
                paramType = cls;
            } else {
                return false;
            }
        }

        if (ProblemDetail.class.isAssignableFrom(paramType)) {
            return false;
        }

        if (returnType.getMethod() != null
            && returnType.getMethod().getAnnotation(ExceptionHandler.class) != null) {
            return false;
        }

        if (paramType.isArray() && paramType.getComponentType() == byte.class) return false;
        if (Resource.class.isAssignableFrom(paramType)) return false;
        if (String.class.isAssignableFrom(paramType)) return false;

        return true;
    }

    @Override
    public Object beforeBodyWrite(
        @Nullable Object body,
        @Nonnull MethodParameter returnType,
        @Nonnull MediaType selectedContentType,
        @Nonnull Class<? extends HttpMessageConverter<?>> selectedConverterType,
        @Nonnull ServerHttpRequest request,
        @Nonnull ServerHttpResponse response) {

        ApiResponseContext context = ApiResponseContext.get();
        List<ApiNotice> notices = context.getNotices();
        List<ServiceStatus> services = context.getServices();

        if (body instanceof ApiResponse<?> apiResponse) {
            var mergedNotices = new ArrayList<ApiNotice>(apiResponse.notices());
            mergedNotices.addAll(notices);

            var mergedServices = new ArrayList<ServiceStatus>(apiResponse.services());
            mergedServices.addAll(services);

            ApiStatus status = resolveStatus(mergedNotices, mergedServices, apiResponse.status());
            return new ApiResponse<>(
                status,
                apiResponse.data(),
                List.copyOf(mergedNotices),
                List.copyOf(mergedServices)
            );
        }

        ApiStatus status = resolveStatus(notices, services, ApiStatus.SUCCESS);

        return new ApiResponse<>(status, body, notices, services);
    }

    /**
     * APPROVAL_REQUIRED takes precedence over other statuses. The body (e.g. the
     * pending ticket) is preserved so the UI can render it alongside the pending
     * indicator.
     */
    private static ApiStatus resolveStatus(
        List<ApiNotice> notices,
        List<ServiceStatus> services,
        ApiStatus cleanStatus
    ) {
        boolean approvalRequired = notices.stream()
            .anyMatch(n -> APPROVAL_REQUIRED_CODE.equals(n.code()));

        if (approvalRequired) {
            return ApiStatus.PENDING;
        }
        if (services.stream().anyMatch(s -> s.status() != ServiceHealth.UP)) {
            return ApiStatus.PARTIAL;
        }
        if (notices.stream().anyMatch(n -> n.severity() == NoticeSeverity.WARN)) {
            return ApiStatus.SUCCESS_WITH_WARNINGS;
        }
        return cleanStatus;
    }

    @Nullable
    private static Class<?> rawClass(Type type) {
        if (type instanceof Class<?> cls) {
            return cls;
        }
        if (type instanceof ParameterizedType pt && pt.getRawType() instanceof Class<?> cls) {
            return cls;
        }
        return null;
    }
}
