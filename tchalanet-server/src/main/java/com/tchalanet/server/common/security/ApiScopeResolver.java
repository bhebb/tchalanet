package com.tchalanet.server.common.security;

import jakarta.servlet.http.HttpServletRequest;

/** Resolves ApiScope for incoming requests based on the request URI. */
public final class ApiScopeResolver {

  private ApiScopeResolver() {}

  public static ApiScope resolve(HttpServletRequest req) {
    String path = req.getRequestURI();
    if (path == null) return ApiScope.PUBLIC;

    // PUBLIC: health, swagger, openapi, and public API
    if (path.startsWith("/actuator")
        || path.startsWith("/swagger-ui")
        || path.equals("/swagger-ui.html")
        || path.startsWith("/v3/api-docs")
        || path.startsWith("/openapi")
        || path.startsWith("/public")
        || path.startsWith("/api/v1/public")) {
      return ApiScope.PUBLIC;
    }

    // INTERNAL: Spring Data REST endpoints (no tenant) under /_sdr
    if (path.startsWith("/api/v1/_sdr") || path.startsWith("/_sdr")) {
      return ApiScope.PLATFORM; // treat as platform (no tenant) but consider it internal
    }

    // PLATFORM (no tenant)
    if (path.startsWith("/api/v1/platform") || path.startsWith("/platform"))
      return ApiScope.PLATFORM;

    // TENANT required
    if (path.startsWith("/api/v1/tenant")
        || path.startsWith("/api/v1/admin")
        || path.startsWith("/tenant")
        || path.startsWith("/admin")) {
      return ApiScope.TENANT;
    }

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
