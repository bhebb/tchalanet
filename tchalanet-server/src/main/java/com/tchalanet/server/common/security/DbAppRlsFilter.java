package com.tchalanet.server.common.security;

import com.tchalanet.server.common.context.RequestContextHolder;
import com.tchalanet.server.common.context.TchRequestContext;
import jakarta.persistence.EntityManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.LOWEST_PRECEDENCE - 5)
@RequiredArgsConstructor
public class DbAppRlsFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(DbAppRlsFilter.class);
  private static final String HEADER_DELETED_VISIBILITY = "X-Deleted-Visibility";

  private final RequestContextHolder requestContextHolder;
  private final EntityManager em;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String tenantId = null;
    boolean isSA = false;

    try {
      TchRequestContext ctx = requestContextHolder.get();
      if (ctx != null) {
        tenantId = ctx.tenantId();
        Set<com.tchalanet.server.common.domain.TchRole> roles = ctx.systemRoles();
        isSA =
            roles != null && roles.contains(com.tchalanet.server.common.domain.TchRole.SUPER_ADMIN);
      }
    } catch (Exception ex) {
      log.debug("No request context available for RLS", ex);
    }

    if (tenantId == null || tenantId.isBlank()) {
      // nothing to set; continue without applying RLS session vars
      filterChain.doFilter(request, response);
      return;
    }

    var visibility = "active";
    var requested = request.getHeader(HEADER_DELETED_VISIBILITY);
    if ((requested == null || requested.isBlank()))
      requested = request.getParameter("deletedVisibility");
    if (isSA && requested != null && !requested.isBlank()) {
      var r = requested.trim().toLowerCase();
      if (r.equals("active") || r.equals("deleted") || r.equals("all")) {
        visibility = r;
      } else {
        log.debug("Ignored invalid deletedVisibility='{}' from request", requested);
      }
    }

    final String tenantToSet = tenantId;
    final String visToSet = visibility;

    try {
      em.unwrap(org.hibernate.Session.class)
          .doWork(
              conn -> {
                try (var st = conn.createStatement()) {
                  st.execute("SELECT set_deleted_visibility('" + visToSet + "')");
                  st.execute("SELECT set_current_tenant('" + tenantToSet.replace("'", "''") + "')");
                }
              });
    } catch (Exception ex) {
      log.error("Failed to set DB RLS session variables", ex);
    }

    filterChain.doFilter(request, response);
  }
}
