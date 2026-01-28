package com.tchalanet.server.core.tenantuser.domain.model;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.types.id.RoleId;
import com.tchalanet.server.common.types.enums.AutonomyLevel;
import com.tchalanet.server.common.types.enums.TenantUserStatus;

public final class TenantUserMembership {
  private final TenantId tenantId;
  private final UserId userId;
  private RoleId roleId;
  private AutonomyLevel autonomyLevel;
  private boolean isOwner;
  private TenantUserStatus status;

  private TenantUserMembership(TenantId tenantId, UserId userId) {
    this.tenantId = tenantId;
    this.userId = userId;
    this.status = TenantUserStatus.ACTIVE;
    this.autonomyLevel = AutonomyLevel.NONE;
    this.isOwner = false;
  }

  public static TenantUserMembership of(TenantId tenantId, UserId userId) {
    return new TenantUserMembership(tenantId, userId);
  }

  public TenantId tenantId() { return tenantId; }
  public UserId userId() { return userId; }
  public RoleId roleId() { return roleId; }
  public AutonomyLevel autonomyLevel() { return autonomyLevel; }
  public boolean isOwner() { return isOwner; }
  public TenantUserStatus status() { return status; }

  // business actions (mutation of in-memory aggregate)
  public TenantUserMembership assignRole(RoleId roleId) {
    this.roleId = roleId;
    return this;
  }

  public TenantUserMembership changeAutonomy(AutonomyLevel level) {
    this.autonomyLevel = level;
    return this;
  }

  public TenantUserMembership markOwner(boolean owner) {
    this.isOwner = owner;
    return this;
  }

  public TenantUserMembership suspend() {
    this.status = TenantUserStatus.SUSPENDED;
    return this;
  }

  public TenantUserMembership reactivate() {
    this.status = TenantUserStatus.ACTIVE;
    return this;
  }
}
