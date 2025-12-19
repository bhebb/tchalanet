package com.tchalanet.server.core.accesscontrol.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Snapshot des infos tenant_user, côté domaine.
 */
public record TenantUserSnapshot(
    UUID tenantId,
    UUID userId,
    UUID roleId,
    AutonomyLevel autonomyLevel,
    boolean owner) {

    public TenantUserSnapshot {
        Objects.requireNonNull(tenantId, "tenantId cannot be null");
        Objects.requireNonNull(userId, "userId cannot be null");
        Objects.requireNonNull(roleId, "roleId cannot be null");
        Objects.requireNonNull(autonomyLevel, "autonomyLevel cannot be null");
    }

    public boolean isOwner() {
        return owner;
    }

    public boolean hasFullAutonomy() {
        return AutonomyLevel.FULL == autonomyLevel;
    }
}
