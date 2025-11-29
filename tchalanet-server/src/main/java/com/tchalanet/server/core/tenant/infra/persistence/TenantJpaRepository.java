package com.tchalanet.server.core.tenant.infra.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TenantJpaRepository extends JpaRepository<TenantJpaEntity, UUID> {
  Optional<TenantJpaEntity> findByCode(String code);

  Optional<TenantJpaEntity> findByName(String name);
}
