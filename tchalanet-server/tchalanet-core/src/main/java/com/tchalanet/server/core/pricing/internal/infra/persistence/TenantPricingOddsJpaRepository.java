package com.tchalanet.server.core.pricing.internal.infra.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TenantPricingOddsJpaRepository
    extends JpaRepository<TenantPricingOddsJpaEntity, UUID> {

  @Query(
      """
        SELECT e FROM TenantPricingOddsJpaEntity e
        WHERE e.tenantId = :tenantId
          AND (:gameCode IS NULL OR e.gameCode = :gameCode)
          AND e.active = true
          AND e.deletedAt IS NULL
        ORDER BY e.gameCode, e.betType, e.betOption, e.pricingVariantCode
        """)
  List<TenantPricingOddsJpaEntity> findActiveByTenant(
      @Param("tenantId") UUID tenantId, @Param("gameCode") String gameCode);

  @Query(
      """
        SELECT e FROM TenantPricingOddsJpaEntity e
        WHERE e.tenantId = :tenantId
          AND e.gameCode = :gameCode
          AND e.pricingVariantCode = :pricingVariantCode
          AND e.deletedAt IS NULL
        """)
  Optional<TenantPricingOddsJpaEntity> findByNaturalKey(
      @Param("tenantId") UUID tenantId,
      @Param("gameCode") String gameCode,
      @Param("pricingVariantCode") String pricingVariantCode);
}
