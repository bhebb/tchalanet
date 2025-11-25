package com.tchalanet.server.tenant.infra.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TenantGameJpaRepository extends JpaRepository<TenantGameJpaEntity, UUID> {
  List<TenantGameJpaEntity> findByTenantId(UUID tenantId);

  Optional<TenantGameJpaEntity> findByTenantIdAndGameId(UUID tenantId, UUID gameId);
}
