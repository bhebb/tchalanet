package com.tchalanet.server.common.persistence.audit;

import com.tchalanet.server.common.context.TchContext;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.persistence.BaseTenantEntity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

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
      TchRequestContext tch = TchContext.currentOrNull();
      if (tch == null) return null;
      return tch.tenantUuid();
    } catch (Exception e) {
      log.debug("Failed to resolve tenant from TchContext", e);
      return null;
    }
  }
}
