package com.tchalanet.server.common.context;

import com.tchalanet.server.common.security.ApiScope;
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

        // INTERNAL: Spring Data REST under /api/v1/_sdr
        if (path.startsWith("/api/v1/_sdr")) {
            return ApiScope.SDR;
        }

        // PLATFORM (no tenant)
        if (path.startsWith("/api/v1/platform")) {
            return ApiScope.PLATFORM;
        }

        // ADMIN required (tenant administration endpoints)
        if (path.startsWith("/api/v1/admin")) {
            return ApiScope.ADMIN;
        }

        // TENANT required (tenant business endpoints)
        if (path.startsWith("/api/v1/tenant")) {
            return ApiScope.TENANT;
        }

        // Default under /api/v1 => PLATFORM-like
        return ApiScope.PLATFORM;
    }

    public static boolean tenantRequired(HttpServletRequest req) {
        var scope = resolve(req);
        return scope == ApiScope.TENANT || scope == ApiScope.ADMIN;
    }

    public static boolean allowDefaultTenant(HttpServletRequest req) {
        return resolve(req) == ApiScope.PUBLIC;
    }
}
