package com.tchalanet.server.core.analytics.internal.infra.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Repository for {@code analytics_seller_terminal_draw}. */
@Repository
public interface AnalyticsSellerTerminalDrawRepository
    extends JpaRepository<AnalyticsSellerTerminalDrawEntity, UUID> {

  Optional<AnalyticsSellerTerminalDrawEntity> findByTenantIdAndSellerTerminalIdAndDrawId(
      UUID tenantId, UUID sellerTerminalId, UUID drawId);

  List<AnalyticsSellerTerminalDrawEntity>
      findByTenantIdAndRefDateBetweenOrderByRefDateDescUpdatedAtDesc(
          UUID tenantId, LocalDate from, LocalDate to);

  List<AnalyticsSellerTerminalDrawEntity>
      findByTenantIdAndSellerTerminalIdAndRefDateOrderByScheduledAtAsc(
          UUID tenantId, UUID sellerTerminalId, LocalDate refDate);

  List<AnalyticsSellerTerminalDrawEntity> findByTenantIdAndSellerTerminalIdAndDrawIdAndRefDate(
      UUID tenantId, UUID sellerTerminalId, UUID drawId, LocalDate refDate);

  @Transactional
  @Modifying
  @Query(
      """
      DELETE FROM AnalyticsSellerTerminalDrawEntity a
       WHERE a.tenantId = :tenantId
         AND a.refDate BETWEEN :from AND :to
      """)
  int deleteTenantRows(
      @Param("tenantId") UUID tenantId,
      @Param("from") LocalDate from,
      @Param("to") LocalDate to);
}
