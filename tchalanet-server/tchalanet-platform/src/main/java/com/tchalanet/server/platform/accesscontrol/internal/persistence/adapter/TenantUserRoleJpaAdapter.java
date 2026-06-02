package com.tchalanet.server.platform.accesscontrol.internal.persistence.adapter;

import com.tchalanet.server.common.types.id.RoleId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.entity.TenantUserRoleJpaEntity;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.repository.AppRoleJpaRepository;
import com.tchalanet.server.platform.accesscontrol.internal.persistence.repository.TenantUserRoleJpaRepository;
import com.tchalanet.server.platform.accesscontrol.internal.service.TenantUserDirectoryPort;
import com.tchalanet.server.platform.accesscontrol.internal.service.TenantUserRoleWriterPort;
import com.tchalanet.server.platform.accesscontrol.internal.service.TenantUserSnapshot;
import com.tchalanet.server.platform.identity.api.model.AutonomyLevel;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class TenantUserRoleJpaAdapter implements TenantUserDirectoryPort, TenantUserRoleWriterPort {

  private final TenantUserRoleJpaRepository tenantUserRoleRepository;
  private final AppRoleJpaRepository appRoleRepository;

  @Override
  public Optional<TenantUserSnapshot> findActiveMembership(TenantId tenantId, UserId userId) {
    var roles = tenantUserRoleRepository.findActiveByTenantAndUser(tenantId.value(), userId.value());
    if (roles.isEmpty()) return Optional.empty();
    var primary = roles.get(0);
    return Optional.of(new TenantUserSnapshot(
        tenantId, userId, RoleId.of(primary.getRoleId()), AutonomyLevel.FULL, false));
  }

  @Override
  public List<RoleId> getUserRolesInTenant(UserId userId, TenantId tenantId) {
    return tenantUserRoleRepository
        .findActiveByTenantAndUser(tenantId.value(), userId.value())
        .stream()
        .map(r -> RoleId.of(r.getRoleId()))
        .toList();
  }

  @Override
  @Transactional
  public void setUserRole(TenantId tenantId, UserId userId, RoleId roleId) {
    if (tenantUserRoleRepository
        .findActiveAssignment(tenantId.value(), userId.value(), roleId.value())
        .isEmpty()) {
      var entity = new TenantUserRoleJpaEntity();
      entity.setTenantId(tenantId.value());
      entity.setUserId(userId.value());
      entity.setRoleId(roleId.value());
      tenantUserRoleRepository.save(entity);
    }
  }

  @Transactional
  public void assign(TenantId tenantId, UserId userId, String roleCode, UserId assignedBy) {
    var role = appRoleRepository.findByCode(roleCode)
        .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleCode));
    var roleId = role.getId();
    if (tenantUserRoleRepository.findActiveAssignment(tenantId.value(), userId.value(), roleId).isEmpty()) {
      var entity = new TenantUserRoleJpaEntity();
      entity.setTenantId(tenantId.value());
      entity.setUserId(userId.value());
      entity.setRoleId(roleId);
      entity.setAssignedBy(assignedBy == null ? null : assignedBy.value());
      tenantUserRoleRepository.save(entity);
    }
  }

  @Transactional
  public void remove(TenantId tenantId, UserId userId, String roleCode) {
    appRoleRepository.findByCode(roleCode).ifPresent(role ->
        tenantUserRoleRepository.softDeleteAssignment(tenantId.value(), userId.value(), role.getId()));
  }
}
