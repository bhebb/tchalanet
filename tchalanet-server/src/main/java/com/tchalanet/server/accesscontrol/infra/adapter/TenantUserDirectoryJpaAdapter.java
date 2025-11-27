package com.tchalanet.server.accesscontrol.infra.adapter;

import com.tchalanet.server.accesscontrol.application.port.out.TenantUserDirectoryPort;
import com.tchalanet.server.accesscontrol.infra.persistence.TenantUserJpaEntity;
import com.tchalanet.server.accesscontrol.infra.persistence.TenantUserJpaRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TenantUserDirectoryJpaAdapter implements TenantUserDirectoryPort {

  private final TenantUserJpaRepository tenantUserRepository;

  @Override
  public Optional<TenantUserSnapshot> findByTenantAndUser(UUID tenantId, UUID userId) {
    return tenantUserRepository.findByTenantIdAndUserId(tenantId, userId).map(this::toSnapshot);
  }

  private TenantUserSnapshot toSnapshot(TenantUserJpaEntity entity) {
    return new TenantUserSnapshot(
        entity.getTenantId(),
        entity.getUserId(),
        entity.getRoleId(), // ⚠️ si tu es encore en varchar "role", adapte le modèle
        entity.getAutonomyLevel(),
        entity.isOwner());
  }
}
