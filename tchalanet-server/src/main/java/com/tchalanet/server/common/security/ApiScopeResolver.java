package com.tchalanet.server.common.security;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Resolves ApiScope for incoming requests based on the request URI.
 */
public final class ApiScopeResolver {

    private ApiScopeResolver() {
    }

    public static ApiScope resolve(HttpServletRequest req) {
        String path = req.getRequestURI();
        if (path == null) return ApiScope.PUBLIC;

        // Infra / docs
        if (path.startsWith("/actuator")
            || path.startsWith("/swagger-ui")
            || path.equals("/swagger-ui.html")
            || path.startsWith("/v3/api-docs")
            || path.startsWith("/openapi")) {
            return ApiScope.PUBLIC;
        }

        // Enforce API base prefix
        if (!path.startsWith("/api/v1/")) {
            // Choice: treat as PUBLIC (but will likely 404) or PLATFORM.
            // I recommend PLATFORM to avoid "public by accident".
            return ApiScope.PLATFORM;
        }

        // PUBLIC
        if (path.startsWith("/api/v1/public")) {
            return ApiScope.PUBLIC;
        }

        // INTERNAL: Spring Data REST (no tenant) under /api/v1/_sdr
        if (path.startsWith("/api/v1/_sdr")) {
            return ApiScope.PLATFORM;
        }

        // PLATFORM (no tenant)
        if (path.startsWith("/api/v1/platform")) {
            return ApiScope.PLATFORM;
        }

        // TENANT required (tenant/admin are tenant-scoped)
        if (path.startsWith("/api/v1/tenant") || path.startsWith("/api/v1/admin")) {
            return ApiScope.TENANT;
        }

        // Default under /api/v1 => PLATFORM-like
        return ApiScope.PLATFORM;
    }

    public static boolean tenantRequired(HttpServletRequest req) {
        return resolve(req) == ApiScope.TENANT;
    }

    public static boolean allowDefaultTenant(HttpServletRequest req) {
        return resolve(req) == ApiScope.PUBLIC;
    }
}
