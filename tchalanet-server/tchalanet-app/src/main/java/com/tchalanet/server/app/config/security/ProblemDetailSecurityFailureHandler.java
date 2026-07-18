package com.tchalanet.server.app.config.security;

import com.tchalanet.server.common.web.api.CommonErrorDescriptors;
import com.tchalanet.server.common.web.error.ProblemDetailResponseWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

/** Maps filter-chain authentication and authorization failures to the shared API error contract. */
final class ProblemDetailSecurityFailureHandler
    implements AuthenticationEntryPoint, AccessDeniedHandler {

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authenticationException)
      throws IOException {
    ProblemDetailResponseWriter.write(
        request, response, CommonErrorDescriptors.AUTHENTICATION_REQUIRED);
  }

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      org.springframework.security.access.AccessDeniedException accessDeniedException)
      throws IOException, ServletException {
    ProblemDetailResponseWriter.write(request, response, CommonErrorDescriptors.ACCESS_DENIED);
  }
}
