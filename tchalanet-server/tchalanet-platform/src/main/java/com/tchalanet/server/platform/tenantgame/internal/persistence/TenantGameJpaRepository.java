package com.tchalanet.server.platform.tenantgame.internal.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TenantGameJpaRepository extends JpaRepository<TenantGameJpaEntity, UUID> {
  Optional<TenantGameJpaEntity> findByTenantIdAndGameId(UUID tenantId, UUID gameId);
  List<TenantGameJpaEntity> findByTenantId(UUID tenantId);
}
