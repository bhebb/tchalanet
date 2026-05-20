package com.tchalanet.server.common.persistence.audit;

import com.tchalanet.server.common.context.TchContext;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.persistence.BaseTenantEntity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TenantEntityListener {

    @PrePersist
    public void prePersist(Object entity) {
        if (!(entity instanceof BaseTenantEntity e)) {
            return;
        }

        UUID currentTenant = resolveTenantUuidOrNull();
        UUID entityTenant = e.getTenantId();

        if (entityTenant != null) {
            if (currentTenant != null && !entityTenant.equals(currentTenant)) {
                throw tenantMismatch("persisting", entity, entityTenant, currentTenant);
            }
            return;
        }

        if (currentTenant == null) {
            throw new IllegalStateException(
                "Missing tenant context while persisting " + entity.getClass().getSimpleName());
        }

        e.setTenantId(currentTenant);
    }

    @PreUpdate
    public void preUpdate(Object entity) {
        if (!(entity instanceof BaseTenantEntity e)) {
            return;
        }

        UUID currentTenant = resolveTenantUuidOrNull();
        UUID entityTenant = e.getTenantId();

        if (currentTenant == null) {
            throw new IllegalStateException(
                "Missing tenant context while updating " + entity.getClass().getSimpleName());
        }

        if (entityTenant == null) {
            throw new IllegalStateException(
                "Missing entity tenant while updating " + entity.getClass().getSimpleName());
        }

        if (!entityTenant.equals(currentTenant)) {
            throw tenantMismatch("updating", entity, entityTenant, currentTenant);
        }
    }

    private IllegalStateException tenantMismatch(
        String action,
        Object entity,
        UUID entityTenant,
        UUID currentTenant
    ) {
        return new IllegalStateException(
            "Tenant mismatch while "
                + action
                + " "
                + entity.getClass().getSimpleName()
                + " (entityTenant="
                + entityTenant
                + ", currentTenant="
                + currentTenant
                + ")");
    }

    private UUID resolveTenantUuidOrNull() {
        try {
            TchRequestContext tch = TchContext.currentOrNull();
            if (tch == null) {
                return null;
            }

            // Prefer effective tenant, especially for SUPER_ADMIN override.
            return tch.effectiveTenantIdRequired().value();

        } catch (Exception e) {
            log.debug("Failed to resolve tenant from TchContext", e);
            return null;
        }
    }
}
