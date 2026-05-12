package com.tchalanet.server.platform.identity.internal.service;

import com.tchalanet.server.common.types.enums.TenantUserStatus;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.RoleId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;

public record TenantMembership(
    TenantId tenantId,
    UserId userId,
    RoleId roleId,
    OutletId outletId,
    TerminalId terminalId,
    TenantUserStatus status,
    boolean owner) {

  public static TenantMembership active(TenantId tenantId, UserId userId) {
    return new TenantMembership(tenantId, userId, null, null, null, TenantUserStatus.ACTIVE, false);
  }

  public TenantMembership assign(RoleId roleId, OutletId outletId, TerminalId terminalId, boolean owner) {
    return new TenantMembership(tenantId, userId, roleId, outletId, terminalId, status, owner);
  }
}
