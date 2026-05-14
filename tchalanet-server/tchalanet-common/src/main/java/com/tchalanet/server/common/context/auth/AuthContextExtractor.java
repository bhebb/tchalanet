package com.tchalanet.server.common.context.auth;

import static com.tchalanet.server.common.context.jwt.SecurityClaims.TENANT_CODE;
import static com.tchalanet.server.common.http.TchHeaders.X_TCH_TENANT_OVERRIDE;
import static com.tchalanet.server.common.http.TchHeaders.X_TENANT_ID;

import com.tchalanet.server.common.security.TchRole;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashSet;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class AuthContextExtractor {

  public ExtractedAuthContext extract(HttpServletRequest req, String defaultTenantCode) {
    var originalTenantCode = normalize(defaultTenantCode);
    var effectiveTenantCode = normalize(defaultTenantCode);
    String keycloakUserId = null;

    var systemRoles = new HashSet<TchRole>();
    var customRoles = new HashSet<String>();
    boolean overridden = false;

    var auth = SecurityContextHolder.getContext().getAuthentication();

    if (!isJwtAuthentication(auth)) {
      return new ExtractedAuthContext(
          originalTenantCode,
          effectiveTenantCode,
          keycloakUserId,
          overridden,
          systemRoles,
          customRoles);
    }

    var jwt = (Jwt) auth.getPrincipal();

    keycloakUserId = jwt.getClaimAsString("sub");

    var jwtTenant = normalize(jwt.getClaimAsString(TENANT_CODE));

    if (StringUtils.isNotBlank(jwtTenant)) {
      originalTenantCode = jwtTenant;
      effectiveTenantCode = jwtTenant;
    }

    var rawRoles = AuthRoleExtractor.collectRoles(jwt);
    var split = AuthRoleExtractor.splitRoles(rawRoles);

    systemRoles.addAll(split.system);
    customRoles.addAll(split.custom);

    if (systemRoles.contains(TchRole.SUPER_ADMIN)) {
      String overrideTenant = normalize(req.getHeader(X_TCH_TENANT_OVERRIDE));

      if (StringUtils.isBlank(overrideTenant)) {
        overrideTenant = normalize(req.getHeader(X_TENANT_ID));
      }

      if (StringUtils.isNotBlank(overrideTenant)) {
        effectiveTenantCode = overrideTenant;
        overridden = true;
      }
    }

    return new ExtractedAuthContext(
        originalTenantCode,
        effectiveTenantCode,
        keycloakUserId,
        overridden,
        systemRoles,
        customRoles);
  }

  private boolean isJwtAuthentication(Authentication auth) {
    return auth != null
        && auth.isAuthenticated()
        && auth.getPrincipal() instanceof Jwt;
  }

  private static String normalize(String value) {
    if (value == null) {
      return null;
    }

    var trimmed = value.trim();

    return trimmed.isBlank() ? null : trimmed;
  }
}
