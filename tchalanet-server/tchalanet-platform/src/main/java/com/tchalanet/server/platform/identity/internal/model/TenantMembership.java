package com.tchalanet.server.platform.identity.internal.model;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.platform.identity.api.model.TenantUserStatus;

public record TenantMembership(
    TenantId tenantId,
    UserId userId,
    TenantUserStatus status,
    boolean owner) {

  public static TenantMembership active(TenantId tenantId, UserId userId) {
    return new TenantMembership(tenantId, userId,  TenantUserStatus.ACTIVE, false);
  }

  public TenantMembership assign(boolean owner) {
    return new TenantMembership(tenantId, userId, status, owner);
  }
}
