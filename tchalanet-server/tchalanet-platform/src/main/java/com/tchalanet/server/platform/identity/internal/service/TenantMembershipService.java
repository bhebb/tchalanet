package com.tchalanet.server.platform.identity.internal.service;

import com.tchalanet.server.common.time.TimeProvider;
import com.tchalanet.server.common.security.TchRole;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.RoleId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.platform.accesscontrol.api.AccessControlApi;
import com.tchalanet.server.platform.accesscontrol.api.model.request.SetTenantUserRoleRequest;
import com.tchalanet.server.platform.identity.internal.persistence.adapter.TenantMembershipJpaAdapter;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TenantMembershipService {

  private final TenantMembershipJpaAdapter memberships;
  private final AccessControlApi accessControlApi;
  private final TimeProvider timeProvider;

  public TchPage<TenantUserRow> list(TenantId tenantId, TchPageRequest pageRequest) {
    return memberships.listByTenant(tenantId, pageRequest);
  }

  public Optional<TenantMembership> findByTenantAndUser(TenantId tenantId, UserId userId) {
    return memberships.findByTenantAndUser(tenantId, userId);
  }

  public Optional<Instant> findCreatedAt(TenantId tenantId, UserId userId) {
    return memberships.findCreatedAt(tenantId, userId);
  }

  public void assign(
      TenantId tenantId,
      UserId userId,
      RoleId roleId,
      OutletId outletId,
      TerminalId terminalId,
      boolean owner) {
    var membership =
        memberships
            .findByTenantAndUser(tenantId, userId)
            .orElseGet(() -> TenantMembership.active(tenantId, userId));
    memberships.upsert(membership.assign(roleId, outletId, terminalId, owner));
  }

  public void unassign(TenantId tenantId, UserId userId) {
    memberships.softDelete(tenantId, userId, timeProvider.nowInstant());
  }

  public void setRole(TenantId tenantId, UserId userId, TchRole role) {
    accessControlApi.setTenantUserRole(new SetTenantUserRoleRequest(tenantId, userId, role));
  }
}
