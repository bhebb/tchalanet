package com.tchalanet.server.core.tenantuser.domain.model;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.types.id.RoleId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.enums.AutonomyLevel;
import com.tchalanet.server.common.types.enums.TenantUserStatus;
import lombok.Getter;

public final class TenantUserMembership {
  private final TenantId tenantId;
  private final UserId userId;
  private RoleId roleId;
  @Getter
  private boolean isOwner;
  private TenantUserStatus status;

  // NEW: workplace
  private OutletId outletId;
  private TerminalId terminalId;

  private TenantUserMembership(TenantId tenantId, UserId userId) {
    this.tenantId = tenantId;
    this.userId = userId;
    this.status = TenantUserStatus.ACTIVE;
    this.isOwner = false;
  }

  public static TenantUserMembership of(TenantId tenantId, UserId userId) {
    return new TenantUserMembership(tenantId, userId);
  }

  public TenantId tenantId() { return tenantId; }
  public UserId userId() { return userId; }
  public RoleId roleId() { return roleId; }

  public OutletId outletId() { return outletId; }
  public TerminalId terminalId() { return terminalId; }

    public TenantUserStatus status() { return status; }

  // business actions (mutation of in-memory aggregate)
  public TenantUserMembership assignRole(RoleId roleId) {
    this.roleId = roleId;
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

  // NEW: workplace assignment
  public TenantUserMembership assignOutlet(OutletId outletId) {
    this.outletId = outletId;
    return this;
  }

  public TenantUserMembership assignTerminal(TerminalId terminalId) {
    this.terminalId = terminalId;
    return this;
  }

  public TenantUserMembership assignWorkplace(OutletId outletId, TerminalId terminalId) {
    this.outletId = outletId;
    this.terminalId = terminalId;
    return this;
  }
}
