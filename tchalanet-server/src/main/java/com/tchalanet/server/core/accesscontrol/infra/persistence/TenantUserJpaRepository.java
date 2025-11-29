package com.tchalanet.server.core.accesscontrol.infra.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TenantUserJpaRepository extends JpaRepository<TenantUserJpaEntity, UUID> {

  @Query(
      "select t from TenantUserJpaEntity t where t.tenantId = :tenantId and t.userId = :userId and t.deletedAt is null")
  Optional<TenantUserJpaEntity> findByTenantIdAndUserId(
      @Param("tenantId") UUID tenantId, @Param("userId") UUID userId);

  @Query("select t from TenantUserJpaEntity t where t.userId = :userId and t.deletedAt is null")
  List<TenantUserJpaEntity> findByUserId(@Param("userId") UUID userId);
}
