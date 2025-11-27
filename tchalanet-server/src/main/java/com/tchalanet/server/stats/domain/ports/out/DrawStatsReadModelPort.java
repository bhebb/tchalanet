package com.tchalanet.server.stats.domain.ports.out;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Outbound Port for reading aggregated DrawStats data. Used by the RebuildTenantDailyStatsUseCase.
 */
public interface DrawStatsReadModelPort {

  List<DrawStatsSummary> findByTenantIdAndDay(UUID tenantId, LocalDate day);

  List<DrawStatsSummary> findByDay(LocalDate day);

  record DrawStatsSummary(
      UUID drawId,
      UUID tenantId,
      long totalTickets,
      BigDecimal totalStake,
      BigDecimal totalPayout) {}
}
