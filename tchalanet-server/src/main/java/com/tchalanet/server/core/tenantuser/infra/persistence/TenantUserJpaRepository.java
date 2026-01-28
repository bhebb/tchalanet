package com.tchalanet.server.core.tenantuser.infra.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantUserJpaRepository extends JpaRepository<TenantUserJpaEntity, UUID> {

  Optional<TenantUserJpaEntity> findByTenantIdAndUserIdAndDeletedAtIsNull(UUID tenantId, UUID userId);

}
