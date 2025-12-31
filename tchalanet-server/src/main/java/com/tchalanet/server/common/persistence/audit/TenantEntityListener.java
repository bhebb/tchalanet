package com.tchalanet.server.common.persistence.audit;

import static com.tchalanet.server.common.constant.ContextKeys.REQUEST_CONTEXT;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.persistence.BaseTenantEntity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

@Slf4j
public class TenantEntityListener {

  @PrePersist
  public void prePersist(Object entity) {
    if (!(entity instanceof BaseTenantEntity e)) return;

    // déjà set (batch/import) => on respecte
    if (e.getTenantId() != null) return;

    UUID tenantUuid = resolveTenantUuidOrNull();
    if (tenantUuid == null) {
      // Recommandé: fail fast pour éviter des données orphelines
      throw new IllegalStateException(
          "Missing tenant context while persisting " + entity.getClass().getSimpleName());
      // Alternative “soft”:
      // log.warn("Missing tenant context while persisting {}", entity.getClass().getSimpleName());
      // return;
    }

    e.setTenantId(tenantUuid);
  }

  @PreUpdate
  public void preUpdate(Object entity) {
    if (!(entity instanceof BaseTenantEntity e)) return;

    var currentTenant = resolveTenantUuidOrNull();
    if (currentTenant == null) {
      // hors request: on ne valide pas (ou throw si tu veux strict)
      return;
    }

    var entityTenant = e.getTenantId();
    if (entityTenant != null && !entityTenant.equals(currentTenant)) {
      throw new IllegalStateException(
          "Tenant mismatch on update for "
              + entity.getClass().getSimpleName()
              + " (entityTenant="
              + entityTenant
              + ", currentTenant="
              + currentTenant
              + ")");
    }
  }

  private UUID resolveTenantUuidOrNull() {
    try {
      var attrs = RequestContextHolder.getRequestAttributes();
      if (attrs == null) return null;

      Object ctx = attrs.getAttribute(REQUEST_CONTEXT, RequestAttributes.SCOPE_REQUEST);
      if (!(ctx instanceof TchRequestContext tch)) return null;

      return tch.tenantUuid();
    } catch (IllegalStateException e) {
      // “No thread-bound request found”
      return null;
    } catch (Exception e) {
      log.debug("Failed to resolve tenant from request context", e);
      return null;
    }
  }
}
