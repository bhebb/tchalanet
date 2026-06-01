package com.tchalanet.server.platform.identity.internal.service;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.platform.identity.api.model.TenantUserStatus;

public record TenantMembership(
    TenantId tenantId,
    UserId userId,
    OutletId outletId,
    TerminalId terminalId,
    TenantUserStatus status,
    boolean owner) {

  public static TenantMembership active(TenantId tenantId, UserId userId) {
    return new TenantMembership(tenantId, userId, null, null, TenantUserStatus.ACTIVE, false);
  }

  public TenantMembership assign(OutletId outletId, TerminalId terminalId, boolean owner) {
    return new TenantMembership(tenantId, userId, outletId, terminalId, status, owner);
  }
}
