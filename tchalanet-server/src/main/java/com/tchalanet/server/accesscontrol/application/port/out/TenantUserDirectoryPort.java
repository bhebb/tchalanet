package com.tchalanet.server.accesscontrol.application.port.out;

import java.util.Optional;
import java.util.UUID;

/** Port de sortie pour l'accès à la table tenant_user (liaison user ↔ tenant). */
public interface TenantUserDirectoryPort {

  final class TenantUserSnapshot {
    private final UUID tenantId;
    private final UUID userId;
    private final UUID roleId;
    private final String autonomyLevel; // none|partial|full
    private final boolean owner;

    public TenantUserSnapshot(
        UUID tenantId, UUID userId, UUID roleId, String autonomyLevel, boolean owner) {
      this.tenantId = tenantId;
      this.userId = userId;
      this.roleId = roleId;
      this.autonomyLevel = autonomyLevel;
      this.owner = owner;
    }

    public UUID tenantId() {
      return tenantId;
    }

    public UUID userId() {
      return userId;
    }

    public UUID roleId() {
      return roleId;
    }

    public String autonomyLevel() {
      return autonomyLevel;
    }

    public boolean isOwner() {
      return owner;
    }
  }

  Optional<TenantUserSnapshot> findByTenantAndUser(UUID tenantId, UUID userId);
}
