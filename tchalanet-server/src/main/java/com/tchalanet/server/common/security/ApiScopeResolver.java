package com.tchalanet.server.common.security;

import jakarta.servlet.http.HttpServletRequest;

/** Resolves ApiScope for incoming requests based on the request URI. */
public final class ApiScopeResolver {

  private ApiScopeResolver() {}

  public static ApiScope resolve(HttpServletRequest req) {
    String path = req.getRequestURI();
    if (path == null) return ApiScope.PUBLIC;

    // PUBLIC
    if (path.startsWith("/actuator")
        || path.startsWith("/swagger-ui")
        || path.equals("/swagger-ui.html")
        || path.startsWith("/v3/api-docs")
        || path.startsWith("/api/v1/openapi")
        || path.startsWith("/api/v1/swagger-ui")
        || path.startsWith("/api/v1/public")) {
      return ApiScope.PUBLIC;
    }

    // PLATFORM (no tenant)
    if (path.startsWith("/api/v1/platform")) return ApiScope.PLATFORM;

    // TENANT required
    if (path.startsWith("/api/v1/tenant") || path.startsWith("/api/v1/admin"))
      return ApiScope.TENANT;

    // Default: authenticated endpoints but not tenant-required => treat as PLATFORM-like
    return ApiScope.PLATFORM;
  }

  public static boolean tenantRequired(HttpServletRequest req) {
    return resolve(req) == ApiScope.TENANT;
  }

  public static boolean allowDefaultTenant(HttpServletRequest req) {
    return resolve(req) == ApiScope.PUBLIC;
  }
}
