package com.tchalanet.server.accesscontrol.infra.security;

import com.tchalanet.server.accesscontrol.application.annotation.RequiresPermission;
import com.tchalanet.server.accesscontrol.application.port.in.CheckUserPermissionsUseCase;
import com.tchalanet.server.common.context.TchRequestContext;
import java.util.Arrays;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
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

  private final CheckUserPermissionsUseCase checkUserPermissionsUseCase;

  public RequiresPermissionAspect(CheckUserPermissionsUseCase checkUserPermissionsUseCase) {
    this.checkUserPermissionsUseCase = checkUserPermissionsUseCase;
  }

  @Around("@annotation(requiresPermission)")
  public Object enforcePermission(ProceedingJoinPoint pjp, RequiresPermission requiresPermission)
      throws Throwable {

    var requested =
        Arrays.stream(requiresPermission.value()).filter(p -> p != null && !p.isBlank()).toList();

    if (!requested.isEmpty()) {
      var authentication = SecurityContextHolder.getContext().getAuthentication();
      var principal = (TchRequestContext) authentication.getPrincipal();

      checkUserPermissionsUseCase.check(principal.tenantUuid(), principal.userUuid(), requested);
    }

    return pjp.proceed();
  }
}
