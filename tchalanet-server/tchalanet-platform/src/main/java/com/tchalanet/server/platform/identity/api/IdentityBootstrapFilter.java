package com.tchalanet.server.platform.identity.api;

import jakarta.servlet.Filter;

/**
 * Servlet filter that resolves the authenticated JWT subject to a provisioned
 * application user and exposes the bootstrapped user id on the request.
 *
 * Public marker exposed by the identity capability so the composition root can
 * wire it into the security filter chain without importing {@code internal/}.
 */
public interface IdentityBootstrapFilter extends Filter {}
