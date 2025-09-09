package com.tchalanet.server.filter;

import com.tchalanet.server.config.context.RequestContextHolder;
import jakarta.persistence.EntityManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.LOWEST_PRECEDENCE - 5) // run after RequestUserContextFilter, before controllers
@RequiredArgsConstructor
public class DbTenantRlsFilter extends OncePerRequestFilter {

  private final RequestContextHolder ctxHolder; // request-scoped bean that reads REQUEST_CONTEXT
  private final EntityManager em;

  @Override
  protected void doFilterInternal(
      HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {
    var ctx = ctxHolder.get();
    if (ctx != null && ctx.tenantId() != null) {
      // Execute on the SAME Hibernate connection for this request/session
      em.unwrap(org.hibernate.Session.class)
          .doWork(
              conn -> {
                try (var st = conn.createStatement()) {
                  // set LOCAL applies to current transaction/connection
                  st.execute(
                      "set local app.current_tenant = '" + ctx.tenantId().replace("'", "''") + "'");
                }
              });
    }
    chain.doFilter(req, res);
  }
}
