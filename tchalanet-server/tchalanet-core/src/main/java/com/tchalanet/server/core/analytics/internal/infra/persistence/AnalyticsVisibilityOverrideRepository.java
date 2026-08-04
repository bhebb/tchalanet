package com.tchalanet.server.core.analytics.internal.infra.persistence;

import com.tchalanet.server.core.analytics.api.model.AnalyticsTrustScopeType;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface AnalyticsVisibilityOverrideRepository
    extends JpaRepository<AnalyticsVisibilityOverrideEntity, UUID> {

  @Query(
      """
      SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END
        FROM AnalyticsVisibilityOverrideEntity o
       WHERE o.scopeType = :scopeType
         AND ((o.tenantId = :tenantId) OR (o.tenantId IS NULL AND :tenantId IS NULL))
         AND ((o.sellerTerminalId = :sellerTerminalId) OR (o.sellerTerminalId IS NULL AND :sellerTerminalId IS NULL))
         AND ((o.drawId = :drawId) OR (o.drawId IS NULL AND :drawId IS NULL))
         AND o.fromDate <= :toDate
         AND o.toDate >= :fromDate
      """)
  boolean existsDisabledForScope(
      @Param("scopeType") AnalyticsTrustScopeType scopeType,
      @Param("tenantId") UUID tenantId,
      @Param("sellerTerminalId") UUID sellerTerminalId,
      @Param("drawId") UUID drawId,
      @Param("fromDate") LocalDate fromDate,
      @Param("toDate") LocalDate toDate);

  @Modifying
  @Transactional
  @Query(
      """
      DELETE FROM AnalyticsVisibilityOverrideEntity o
       WHERE o.scopeType = :scopeType
         AND ((o.tenantId = :tenantId) OR (o.tenantId IS NULL AND :tenantId IS NULL))
         AND ((o.sellerTerminalId = :sellerTerminalId) OR (o.sellerTerminalId IS NULL AND :sellerTerminalId IS NULL))
         AND ((o.drawId = :drawId) OR (o.drawId IS NULL AND :drawId IS NULL))
         AND o.fromDate = :fromDate
         AND o.toDate = :toDate
      """)
  int deleteExactScope(
      @Param("scopeType") AnalyticsTrustScopeType scopeType,
      @Param("tenantId") UUID tenantId,
      @Param("sellerTerminalId") UUID sellerTerminalId,
      @Param("drawId") UUID drawId,
      @Param("fromDate") LocalDate fromDate,
      @Param("toDate") LocalDate toDate);
}
