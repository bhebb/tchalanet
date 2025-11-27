package com.tchalanet.server.common.security;

import static com.tchalanet.server.common.domain.AppConstants.REQUEST_CONTEXT;
import static com.tchalanet.server.common.domain.AppConstants.TENANT_ID_CLAIMS;

import com.tchalanet.server.accesscontrol.domain.model.TchRole;
import com.tchalanet.server.accesscontrol.infra.security.RoleUtils;
import com.tchalanet.server.common.config.ApiProperties;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.usecase.ResolveTenantUseCase;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Builds a per-request RequestContext: - originalTenantCode: from JWT claim (fallback to default).
 * - tenantId (effective): original or overridden (SUPER_ADMIN only) via X-Tenant-Id / ?tenantId=. -
 * roles: both system (TchRole) and custom role names preserved. - tenantOverridden: flag for
 * SA-in-tenant mode.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10) // ensure this runs after Spring Security's JWT filter
@RequiredArgsConstructor
public class RequestUserContextFilter extends OncePerRequestFilter {

  private static final String X_REQUEST_ID = "X-Request-Id";
  private static final String X_FORWARDED_FOR = "X-Forwarded-For";

  private final ApiProperties props; // provides defaultTenant(), etc.
  private final ResolveTenantUseCase tenantResolver;

  @Override
  protected void doFilterInternal(
      HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {

    var originalTenantCode = props.defaultTenant();
    UUID originalTenantId = null;
    var effectiveTenantCode = originalTenantCode;
    UUID effectiveTenantId = null;

    String userId = null;
    var locale = req.getLocale();
    var requestId =
        Optional.ofNullable(req.getHeader(X_REQUEST_ID)).orElse(UUID.randomUUID().toString());
    var clientIp =
        Optional.ofNullable(req.getHeader(X_FORWARDED_FOR)).orElseGet(req::getRemoteAddr);

    var systemRoles = new HashSet<TchRole>();
    Set<String> customRoles = new HashSet<>();
    var overridden = false;

    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof Jwt jwt) {
      userId = jwt.getSubject();

      var jwtTenant = jwt.getClaimAsString(TENANT_ID_CLAIMS);
      if (jwtTenant != null && !jwtTenant.isBlank()) {
        originalTenantCode = jwtTenant;
        effectiveTenantCode = jwtTenant;
        var resolved = tenantResolver.resolveIdByCode(jwtTenant);
        if (resolved.isPresent()) originalTenantId = resolved.get();
      }

      var rawRoles = RoleUtils.collectRoles(jwt);
      var split = RoleUtils.splitRoles(rawRoles);
      systemRoles.addAll(split.system);
      customRoles = split.custom;

      var isSA = systemRoles.contains(TchRole.SUPER_ADMIN);
      var overrideTenant =
          Optional.ofNullable(req.getHeader("X-Tenant-Id")).orElse(req.getParameter("tenantId"));
      if (isSA && overrideTenant != null && !overrideTenant.isBlank()) {
        effectiveTenantCode = overrideTenant;
        overridden = true;
        var resolved = tenantResolver.resolveIdByCode(overrideTenant);
        if (resolved.isPresent()) effectiveTenantId = resolved.get();
      }
    }

    if (effectiveTenantId == null) effectiveTenantId = originalTenantId;

    var ctx =
        new TchRequestContext(
            originalTenantCode,
            originalTenantId,
            effectiveTenantCode,
            effectiveTenantId,
            userId,
            systemRoles,
            customRoles,
            locale,
            requestId,
            clientIp,
            overridden);

    req.setAttribute(REQUEST_CONTEXT, ctx);

    MDC.put("tenant_original", originalTenantCode != null ? originalTenantCode : "-");
    MDC.put("tenant_effective", effectiveTenantCode != null ? effectiveTenantCode : "-");
    MDC.put("tenant_overridden", String.valueOf(overridden));
    MDC.put("user", userId != null ? userId : "-");
    MDC.put("reqId", requestId);

    try {
      chain.doFilter(req, res);
    } finally {
      MDC.clear();
    }
  }
}
