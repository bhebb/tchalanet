package com.tchalanet.server.core.accesscontrol.infra.security;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.core.accesscontrol.application.annotation.RequiresPermission;
import com.tchalanet.server.core.accesscontrol.application.query.model.CheckUserPermissionsQuery;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Aspect centralisant l'application de @RequiresPermission sur les controllers.
 *
 * <p>- Lit les permissions à partir de l'annotation. - Récupère tenantId / userId depuis le
 * principal. - Appelle CheckUserPermissionsUseCase. - Sémantique : toutes les permissions demandées
 * doivent être accordées (AND).
 */
@Aspect
@Component
public class RequiresPermissionAspect {

  private final QueryBus queryBus;

  public RequiresPermissionAspect(QueryBus queryBus) {
    this.queryBus = queryBus;
  }

  @Around("@annotation(requiresPermission)")
  public Object enforcePermission(ProceedingJoinPoint pjp, RequiresPermission requiresPermission)
      throws Throwable {

    var requested =
        Arrays.stream(requiresPermission.value()).filter(p -> p != null && !p.isBlank()).toList();

    if (!requested.isEmpty()) {
      var authentication = SecurityContextHolder.getContext().getAuthentication();
      Object principal = authentication.getPrincipal();
      if (!(principal instanceof TchRequestContext ctx)) {
        throw new AccessDeniedException("Missing TchRequestContext principal");
      }

      UUID tenantId = ctx.tenantUuid();
      UUID userId = ctx.userUuid();

      if (tenantId == null || userId == null) {
        throw new AccessDeniedException("Missing tenant or user in request context");
      }

      var query = new CheckUserPermissionsQuery(tenantId, userId, Set.copyOf(requested));
      Boolean allowed = queryBus.send(query);

      if (allowed == null || !allowed) {
        throw new AccessDeniedException("Access denied: missing required permissions");
      }
    }

    return pjp.proceed();
  }
}
