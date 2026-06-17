package com.tchalanet.server.platform.identity.api;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Servlet filter that resolves the authenticated JWT subject to a provisioned
 * application user and exposes the bootstrapped user id on the request.
 * <p>
 * Public marker exposed by the identity capability so the composition root can
 * wire it into the security filter chain without importing {@code internal/}.
 */
public interface IdentityBootstrapStep {
    void bootstrap(HttpServletRequest request);
}
