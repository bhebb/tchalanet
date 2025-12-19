package com.tchalanet.server.core.accesscontrol.infra.persistence;

import com.tchalanet.server.core.accesscontrol.application.port.out.TenantUserDirectoryPort;
import com.tchalanet.server.core.accesscontrol.domain.model.AutonomyLevel;
import com.tchalanet.server.core.accesscontrol.domain.model.TenantUserSnapshot;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TenantUserDirectoryJpaAdapter implements TenantUserDirectoryPort {

  private final TenantUserRepository tenantUserRepository;

  @Override
  public Optional<TenantUserSnapshot> findActiveMembership(UUID tenantId, UUID userId) {
    if (tenantId == null || userId == null) {
      return Optional.empty();
    }
    return tenantUserRepository.findByTenantIdAndUserId(tenantId, userId).stream()
        .filter(entity -> entity.getDeletedAt() == null)
        .findFirst()
        .map(
            entity ->
                new TenantUserSnapshot(
                    entity.getTenantId(),
                    entity.getUserId(),
                    entity.getRoleId(),
                    AutonomyLevel.valueOf(entity.getAutonomyLevel()),
                    Boolean.TRUE.equals(entity.getOwner())));
  }
}
