package com.tchalanet.server.core.tenantuser.application.port.out;

import com.tchalanet.server.core.tenantuser.domain.model.TenantUserMembership;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;

import java.time.Instant;

public interface TenantUserWriterPort {
  TenantUserMembership upsertMembership(TenantUserMembership m);

  void softDeleteMembership(TenantId tenantId, UserId userId, Instant when);
}
