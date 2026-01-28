package com.tchalanet.server.core.tenantuser.infra.persistence;

import com.tchalanet.server.core.tenantuser.application.port.out.TenantUserWriterPort;
import com.tchalanet.server.core.tenantuser.domain.model.TenantUserMembership;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TenantUserPersistenceAdapter implements TenantUserWriterPort {

  private final TenantUserJpaRepository jpa;

  @Override
  public TenantUserMembership upsertMembership(TenantUserMembership m) {
    var entityOpt = jpa.findByTenantIdAndUserIdAndDeletedAtIsNull(m.tenantId().value(), m.userId().value());
    TenantUserJpaEntity e = entityOpt.orElseGet(TenantUserJpaEntity::new);
    e.setTenantId(m.tenantId().value());
    e.setUserId(m.userId().value());
    e.setRoleId(m.roleId() == null ? null : m.roleId().value());
    e.setAutonomyLevel(m.autonomyLevel());
    e.setIsOwner(m.isOwner());
    e.setStatus(m.status());
    var saved = jpa.save(e);
    return toDomain(saved);
  }

  @Override
  public void softDeleteMembership(TenantId tenantId, UserId userId, Instant when) {
    jpa.findByTenantIdAndUserIdAndDeletedAtIsNull(tenantId.value(), userId.value()).ifPresent(e -> {
      e.setDeletedAt(when);
      jpa.save(e);
    });
  }

  private TenantUserMembership toDomain(TenantUserJpaEntity e) {
    var m = TenantUserMembership.of(TenantId.of(e.getTenantId()), UserId.of(e.getUserId()));
    if (e.getRoleId() != null) m.assignRole(com.tchalanet.server.common.types.id.RoleId.of(e.getRoleId()));
    if (e.getAutonomyLevel() != null) m.changeAutonomy(e.getAutonomyLevel());
    if (e.getIsOwner() != null) m.markOwner(e.getIsOwner());
    if (e.getStatus() != null && e.getStatus() == com.tchalanet.server.common.types.enums.TenantUserStatus.SUSPENDED) m.suspend();
    return m;
  }
  // persistence adapter is write-only; QueryAdapter provides enriched read models (JOINs)
}
