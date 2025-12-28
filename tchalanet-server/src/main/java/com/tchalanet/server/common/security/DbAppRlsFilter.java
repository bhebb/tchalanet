package com.tchalanet.server.common.security;

import static com.tchalanet.server.common.constant.TchHeaders.X_DELETED_VISIBILITY;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.TchRequestContextHolder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.LOWEST_PRECEDENCE - 5)
@RequiredArgsConstructor
@Slf4j
public class DbAppRlsFilter extends OncePerRequestFilter {

  private final TchRequestContextHolder requestContextHolder;
  @PersistenceContext private EntityManager em;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    var rlsContext = resolveRlsContext();
    // If we don't have a tenant UUID, don't attempt to apply DB RLS session variables
    if (rlsContext.tenantUuid == null) {
      // Pas de tenant UUID → pas de RLS à appliquer
      filterChain.doFilter(request, response);
      return;
    }

    var visibility = resolveVisibility(request, rlsContext.isSuperAdmin);
    applyRlsSessionVariables(rlsContext.tenantUuid, visibility);

    filterChain.doFilter(request, response);
  }

  // ---------------------------------------------------------------------
  // Résolution du contexte (tenant + rôle)
  // ---------------------------------------------------------------------

  private RlsContext resolveRlsContext() {
    String tenantCode = null;
    UUID tenantUuid = null;
    boolean isSuperAdmin = false;

    try {
      TchRequestContext ctx = requestContextHolder.get();
      if (ctx != null) {
        tenantCode = ctx.effectiveTenantCode();
        tenantUuid = ctx.effectiveTenantUuid();
        Set<TchRole> roles = ctx.systemRoles();
        isSuperAdmin = roles != null && roles.contains(TchRole.SUPER_ADMIN);
      }
    } catch (Exception ex) {
      log.debug("No request context available for RLS", ex);
    }

    return new RlsContext(tenantCode, tenantUuid, isSuperAdmin);
  }

  // ---------------------------------------------------------------------
  // Résolution de la visibilité des enregistrements supprimés
  // ---------------------------------------------------------------------

  private String resolveVisibility(HttpServletRequest request, boolean isSuperAdmin) {
    // valeur par défaut
    var visibility = "active";

    // header prioritaire, sinon query param
    var requested = request.getHeader(X_DELETED_VISIBILITY);
    if (requested == null || requested.isBlank()) {
      requested = request.getParameter("deletedVisibility");
    }

    if (!isSuperAdmin || requested == null || requested.isBlank()) {
      return visibility;
    }

    var v = requested.trim().toLowerCase();
    if (v.equals("active") || v.equals("deleted") || v.equals("all")) {
      return v;
    }

    log.debug("Ignored invalid deletedVisibility='{}' from request", requested);
    return visibility;
  }

  // ---------------------------------------------------------------------
  // Application des variables de session RLS côté DB
  // ---------------------------------------------------------------------

  private void applyRlsSessionVariables(UUID tenantUuid, String visibility) {
    final String vis = visibility; // déjà whitelisté

    try {
      em.unwrap(Session.class)
          .doWork(
              conn -> {
                // Use parameterized statements to avoid passing empty strings to UUID casts
                try (PreparedStatement stVisibility = conn.prepareStatement("SELECT set_deleted_visibility(?)");
                    PreparedStatement stTenant = conn.prepareStatement("SELECT set_current_tenant(?)")) {

                  // set_deleted_visibility
                  stVisibility.setString(1, vis);
                  stVisibility.execute();

                  // set_current_tenant expects a UUID; use setObject with UUID to ensure proper binding
                  if (tenantUuid != null) {
                    try {
                      stTenant.setObject(1, tenantUuid);
                      stTenant.execute();
                    } catch (Exception e) {
                      // Defensive: if binding fails, log and skip RLS tenant setting rather than throw
                      log.warn("Skipping set_current_tenant due to invalid tenant UUID: {}", tenantUuid, e);
                    }
                  }
                }
              });
    } catch (Exception ex) {
      log.error("Failed to set DB RLS session variables", ex);
    }
  }

  // ---------------------------------------------------------------------
  // Petit record interne pour clarifier les retours de resolveRlsContext
  // ---------------------------------------------------------------------

  private record RlsContext(String tenantCode, UUID tenantUuid, boolean isSuperAdmin) {}
}
