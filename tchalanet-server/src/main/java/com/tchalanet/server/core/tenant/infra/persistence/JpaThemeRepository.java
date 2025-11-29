package com.tchalanet.server.core.tenant.infra.persistence;

import com.tchalanet.server.core.tenant.domain.model.ThemeStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaThemeRepository extends JpaRepository<ThemeJpaEntity, UUID> {
  Optional<ThemeJpaEntity> findFirstByTenantIdAndStatusOrderByUpdatedAtDesc(
      UUID tenantId, ThemeStatus status);

  List<ThemeJpaEntity> findByTenantId(UUID tenantId);

  List<ThemeJpaEntity> findByTenantIdIsNull();
}
