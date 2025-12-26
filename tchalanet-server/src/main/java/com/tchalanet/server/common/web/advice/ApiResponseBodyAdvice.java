package com.tchalanet.server.common.web.advice;

import com.tchalanet.server.common.web.api.ApiNotice;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.api.ApiStatus;
import com.tchalanet.server.common.web.api.ServiceStatus;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.List;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * ResponseBodyAdvice that automatically wraps successful responses in ApiResponse. Only wraps 2xx
 * responses, leaves errors (ProblemDetail) unchanged.
 */
@RestControllerAdvice
public class ApiResponseBodyAdvice implements ResponseBodyAdvice<Object> {

  @Override
  public boolean supports(
      @Nonnull MethodParameter returnType,
      @Nonnull Class<? extends HttpMessageConverter<?>> converterType) {
    // Don't wrap if already ApiResponse or ProblemDetail
    Class<?> paramType = returnType.getParameterType();
    return !ApiResponse.class.isAssignableFrom(paramType)
        && !ProblemDetail.class.isAssignableFrom(paramType);
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
