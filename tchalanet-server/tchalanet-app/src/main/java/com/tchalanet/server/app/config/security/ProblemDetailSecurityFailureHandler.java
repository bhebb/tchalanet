package com.tchalanet.server.app.config.security;

import com.tchalanet.server.common.observability.TchTraceIds;
import com.tchalanet.server.common.web.api.CommonErrorDescriptors;
import com.tchalanet.server.common.web.error.ProblemDetailResponseWriter;
import com.tchalanet.server.common.web.observability.RequiredRequestIdFilter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

/** Maps filter-chain authentication and authorization failures to the shared API error contract. */
@Slf4j
final class ProblemDetailSecurityFailureHandler
    implements AuthenticationEntryPoint, AccessDeniedHandler {

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authenticationException)
      throws IOException {
    logRejection(request, CommonErrorDescriptors.AUTHENTICATION_REQUIRED);
    ProblemDetailResponseWriter.write(
        request, response, CommonErrorDescriptors.AUTHENTICATION_REQUIRED);
  }

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      org.springframework.security.access.AccessDeniedException accessDeniedException)
      throws IOException, ServletException {
    logRejection(request, CommonErrorDescriptors.ACCESS_DENIED);
    ProblemDetailResponseWriter.write(request, response, CommonErrorDescriptors.ACCESS_DENIED);
  }

  private static void logRejection(
      HttpServletRequest request,
      com.tchalanet.server.common.web.error.ErrorDescriptor descriptor) {
    var requestId = request.getAttribute(RequiredRequestIdFilter.ATTR_REQUEST_ID);
    log.warn(
        "security.request.rejected status={} code={} method={} path={} requestId={} traceId={} spanId={}",
        descriptor.expectedStatus().value(),
        descriptor.code(),
        request.getMethod(),
        request.getRequestURI(),
        requestId,
        TchTraceIds.currentTraceId(),
        TchTraceIds.currentSpanId());
  }
}
