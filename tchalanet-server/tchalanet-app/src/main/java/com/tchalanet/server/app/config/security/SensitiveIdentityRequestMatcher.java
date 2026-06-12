package com.tchalanet.server.app.config.security;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Set;
import org.springframework.security.web.util.matcher.RequestMatcher;

import static com.tchalanet.server.common.http.TchHeaders.X_TCH_TENANT_OVERRIDE;
import static com.tchalanet.server.common.http.TchHeaders.X_TENANT_ID;

final class SensitiveIdentityRequestMatcher implements RequestMatcher {

  private static final Set<String> MUTATING_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

  @Override
  public boolean matches(HttpServletRequest request) {
    if (hasText(request.getHeader(X_TCH_TENANT_OVERRIDE))
        || hasText(request.getHeader(X_TENANT_ID))) {
      return true;
    }
    return MUTATING_METHODS.contains(request.getMethod());
  }

  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
