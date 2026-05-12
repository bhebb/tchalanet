package com.tchalanet.server.platform.identity.internal.persistence.repository;

import com.tchalanet.server.platform.identity.internal.persistence.entity.TenantUserJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TenantUserJpaRepository extends JpaRepository<TenantUserJpaEntity, UUID> {

  Optional<TenantUserJpaEntity> findByTenantIdAndUserIdAndDeletedAtIsNull(UUID tenantId, UUID userId);

  Optional<TenantUserJpaEntity> findByUserIdAndDeletedAtIsNull(UUID userId);
}
