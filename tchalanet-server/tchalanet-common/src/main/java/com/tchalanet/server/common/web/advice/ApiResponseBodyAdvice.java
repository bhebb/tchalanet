package com.tchalanet.server.common.web.advice;


import com.tchalanet.server.common.web.api.ApiNotice;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.api.ApiStatus;
import com.tchalanet.server.common.web.api.ServiceStatus;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.core.MethodParameter;
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
import java.util.List;

/**
 * ResponseBodyAdvice that automatically wraps successful responses in ApiResponse. Only wraps 2xx
 * responses, leaves errors (ProblemDetail) unchanged.
 */
@RestControllerAdvice(basePackages = "com.tchalanet.server")
public class ApiResponseBodyAdvice implements ResponseBodyAdvice<Object> {

  @Override
  public boolean supports(
      @Nonnull MethodParameter returnType,
      @Nonnull Class<? extends HttpMessageConverter<?>> converterType) {

    // Avoid wrapping when the selected message converter is handling Strings/bytes/resources
    // — do this early to prevent inspecting generics and avoid wrapping into ApiResponse which
    // would later be passed to a String converter and cause ClassCastException.
    if (StringHttpMessageConverter.class.isAssignableFrom(converterType)) {
      return false;
    }
    if (ByteArrayHttpMessageConverter.class.isAssignableFrom(converterType)) {
      return false;
    }
    if (ResourceHttpMessageConverter.class.isAssignableFrom(converterType)) {
      return false;
    }

    // Determine the actual response type. If controller method returns ResponseEntity<T>
    // we want to inspect T.
    Class<?> paramType = returnType.getParameterType();

    if (ResponseEntity.class.isAssignableFrom(paramType)) {
      Type gen = returnType.getGenericParameterType();
      if (gen instanceof ParameterizedType pt) {
        Type arg = pt.getActualTypeArguments()[0];
        if (arg instanceof Class<?>) {
          paramType = (Class<?>) arg;
        } else {
          // Can't determine generic type concretely - don't wrap to be safe
          return false;
        }
      } else {
        return false;
      }
    }

    // Don't wrap if already ApiResponse or ProblemDetail
    if (ApiResponse.class.isAssignableFrom(paramType)
        || ProblemDetail.class.isAssignableFrom(paramType)) {
      return false;
    }

    // Don't wrap methods that are ExceptionHandler (global or controller-level)
    if (returnType.getMethod() != null
        && returnType.getMethod().getAnnotation(ExceptionHandler.class) != null) {
      return false;
    }

    // Don't wrap raw binary responses (byte[])
    if (paramType.isArray() && paramType.getComponentType() == byte.class) {
      return false;
    }

    // Don't wrap Resource responses (files, streams)
    if (Resource.class.isAssignableFrom(paramType)) {
      return false;
    }
    if (String.class.isAssignableFrom(paramType)) {
      return false;
    }

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
