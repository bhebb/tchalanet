package com.tchalanet.server.platform.accesscontrol.api;

import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Servlet filter that reads {@code BootstrappedActor} from the request, resolves DB-owned
 * roles and permissions, attaches {@code ResolvedAccessContext} to the request, and replaces
 * provider-issued Spring Security authorities with {@code ROLE_*}, {@code PERM_*}, and
 * {@code ACTOR_*} entries.
 *
 * <p>Marker interface exposed by the access-control capability so the composition root
 * ({@code tchalanet-app}) can wire this filter after {@code IdentityBootstrapFilter} without
 * importing {@code internal/}.
 */
public interface AccessResolutionStep {
    void resolve(HttpServletRequest request);
}
