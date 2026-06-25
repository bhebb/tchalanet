package com.tchalanet.server.core.analytics.internal.infra.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repository for {@code analytics_seller_terminal_draw}. */
@Repository
public interface AnalyticsSellerTerminalDrawRepository
    extends JpaRepository<AnalyticsSellerTerminalDrawEntity, UUID> {

  Optional<AnalyticsSellerTerminalDrawEntity> findByTenantIdAndSellerTerminalIdAndDrawId(
      UUID tenantId,
      UUID sellerTerminalId,
      UUID drawId);

  List<AnalyticsSellerTerminalDrawEntity> findByTenantIdAndRefDateBetweenOrderByRefDateDescUpdatedAtDesc(
      UUID tenantId,
      LocalDate from,
      LocalDate to);
}
