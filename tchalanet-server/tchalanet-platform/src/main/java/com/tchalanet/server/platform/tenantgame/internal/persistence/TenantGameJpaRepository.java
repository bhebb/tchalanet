package com.tchalanet.server.platform.tenantgame.internal.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TenantGameJpaRepository extends JpaRepository<TenantGameJpaEntity, UUID> {
    Optional<TenantGameJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);
    Optional<TenantGameJpaEntity> findByTenantIdAndGameId(UUID tenantId, UUID gameId);
    Optional<TenantGameJpaEntity> findByTenantIdAndGameCode(UUID tenantId, String gameCode);
    List<TenantGameJpaEntity> findByTenantId(UUID tenantId);
    List<TenantGameJpaEntity> findByTenantIdAndEnabled(UUID tenantId, boolean enabled);
}
