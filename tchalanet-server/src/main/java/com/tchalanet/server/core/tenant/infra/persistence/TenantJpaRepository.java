package com.tchalanet.server.core.tenant.infra.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TenantJpaRepository extends JpaRepository<TenantJpaEntity, UUID> {
  Optional<TenantJpaEntity> findByCode(String code);

  Optional<TenantJpaEntity> findByCodeIgnoreCaseAndDeletedAtIsNull(String code);

  boolean existsByCodeIgnoreCaseAndDeletedAtIsNull(String code);

  @Modifying
  @Query(
      "update TenantJpaEntity t set t.activeThemeId = :themeId where t.id = :tenantId and t.deletedAt is null")
  void updateActiveThemeId(@Param("tenantId") UUID tenantId, @Param("themeId") UUID themeId);

  long countByDeletedAtIsNull();

  long countByStatusAndDeletedAtIsNull(com.tchalanet.server.common.types.enums.TenantStatus status);
}
