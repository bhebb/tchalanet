package com.tchalanet.server.core.accesscontrol.infra.persistence;

import com.tchalanet.server.common.types.enums.AutonomyLevel;
import com.tchalanet.server.common.types.id.RoleId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.accesscontrol.application.port.out.TenantUserDirectoryPort;
import com.tchalanet.server.core.accesscontrol.domain.model.TenantUserSnapshot;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TenantUserDirectoryJpaAdapter implements TenantUserDirectoryPort {

  private final TenantUserRepository tenantUserRepository;

  @Override
  public Optional<TenantUserSnapshot> findActiveMembership(TenantId tenantId, UserId userId) {
    if (tenantId == null || userId == null) {
      return Optional.empty();
    }
    String userIdStr = userId.toString();
    return tenantUserRepository.findByTenantIdAndUserId(tenantId.uuid(), userIdStr).stream()
        .filter(entity -> entity.getDeletedAt() == null)
        .findFirst()
        .map(
            entity ->
                new TenantUserSnapshot(
                    TenantId.of(entity.getTenantId()),
                    UserId.of(entity.getUserId()),
                    RoleId.of(entity.getRoleId()),
                    AutonomyLevel.valueOf(entity.getAutonomyLevel()),
                    Boolean.TRUE.equals(entity.getOwner())));
  }

  @Override
  public List<RoleId> getUserRolesInTenant(UserId userId, TenantId tenantId) {
    String userIdStr = userId == null ? null : userId.toString();
    return tenantUserRepository.findByTenantIdAndUserId(tenantId.uuid(), userIdStr).stream()
        .map(TenantUserEntity::getRoleId)
        .map(RoleId::of)
        .toList();
  }
}
