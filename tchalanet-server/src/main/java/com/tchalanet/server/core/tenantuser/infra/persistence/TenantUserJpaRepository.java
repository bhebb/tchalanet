package com.tchalanet.server.core.tenantuser.infra.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantUserJpaRepository extends JpaRepository<TenantUserJpaEntity, UUID> {

  Optional<TenantUserJpaEntity> findByTenantIdAndUserIdAndDeletedAtIsNull(UUID tenantId, UUID userId);

  Optional<TenantUserJpaEntity> findByUserIdAndDeletedAtIsNull(UUID userId);

}
