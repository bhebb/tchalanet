package com.tchalanet.server.common.security;

import static com.tchalanet.server.common.constant.TchHeaders.X_DELETED_VISIBILITY;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.TchRequestContextHolder;
import com.tchalanet.server.core.accesscontrol.domain.model.TchRole;
import jakarta.persistence.EntityManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Statement;
import java.util.Set;
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
  private final EntityManager em;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    var rlsContext = resolveRlsContext();
    if (rlsContext.tenantCode == null || rlsContext.tenantCode.isBlank()) {
      // Pas de tenant → pas de RLS à appliquer
      filterChain.doFilter(request, response);
      return;
    }

    var visibility = resolveVisibility(request, rlsContext.isSuperAdmin);
    applyRlsSessionVariables(rlsContext.tenantCode, visibility);

    filterChain.doFilter(request, response);
  }

  // ---------------------------------------------------------------------
  // Résolution du contexte (tenant + rôle)
  // ---------------------------------------------------------------------

  private RlsContext resolveRlsContext() {
    String tenantCode = null;
    boolean isSuperAdmin = false;

    try {
      TchRequestContext ctx = requestContextHolder.get();
      if (ctx != null) {
        tenantCode = ctx.effectiveTenantCode();
        Set<TchRole> roles = ctx.systemRoles();
        isSuperAdmin = roles != null && roles.contains(TchRole.SUPER_ADMIN);
      }
    } catch (Exception ex) {
      log.debug("No request context available for RLS", ex);
    }

    return new RlsContext(tenantCode, isSuperAdmin);
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

  private void applyRlsSessionVariables(String tenantCode, String visibility) {
    final String safeTenant = tenantCode.replace("'", "''");
    final String vis = visibility; // déjà whitelisté

    try {
      em.unwrap(Session.class)
          .doWork(
              conn -> {
                try (Statement st = conn.createStatement()) {
                  st.execute("SELECT set_deleted_visibility('" + vis + "')");
                  st.execute("SELECT set_current_tenant('" + safeTenant + "')");
                }
              });
    } catch (Exception ex) {
      log.error("Failed to set DB RLS session variables", ex);
    }
  }

  // ---------------------------------------------------------------------
  // Petit record interne pour clarifier les retours de resolveRlsContext
  // ---------------------------------------------------------------------

  private record RlsContext(String tenantCode, boolean isSuperAdmin) {}
}
