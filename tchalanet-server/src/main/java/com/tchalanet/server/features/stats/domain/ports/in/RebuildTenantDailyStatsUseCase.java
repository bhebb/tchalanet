package com.tchalanet.server.features.stats.domain.ports.in;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Inbound Port for rebuilding or aggregating tenant daily statistics. This is typically run as a
 * batch job.
 */
public interface RebuildTenantDailyStatsUseCase {
  /**
   * Aggregates draw statistics for a given day into tenant daily statistics.
   *
   * @param tenantId The ID of the tenant to process.
   * @param day The specific day for which to rebuild stats.
   * @return The number of tenant daily stats records updated/created.
   */
  int rebuildForDay(UUID tenantId, LocalDate day);

  /**
   * Aggregates draw statistics for all tenants for a given day.
   *
   * @param day The specific day for which to rebuild stats.
   * @return The number of tenant daily stats records updated/created.
   */
  int rebuildForAllTenantsForDay(LocalDate day);
}
