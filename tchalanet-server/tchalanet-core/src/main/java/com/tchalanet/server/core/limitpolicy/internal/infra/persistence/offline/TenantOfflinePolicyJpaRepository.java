package com.tchalanet.server.core.limitpolicy.internal.infra.persistence.offline;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantOfflinePolicyJpaRepository
    extends JpaRepository<TenantOfflinePolicyJpaEntity, UUID> {

    Optional<TenantOfflinePolicyJpaEntity> findByTenantId(UUID tenantId);
}
