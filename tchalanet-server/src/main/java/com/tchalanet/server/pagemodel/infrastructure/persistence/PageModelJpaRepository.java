package com.tchalanet.server.pagemodel.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PageModelJpaRepository extends JpaRepository<PageModelEntity, UUID> {

  Optional<PageModelEntity> findFirstByTenantIdAndCodeAndLang(
      UUID tenantId, String code, String lang);
}
