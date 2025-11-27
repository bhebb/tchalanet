package com.tchalanet.server.stats.domain.ports.in;

import com.tchalanet.server.stats.domain.model.TenantDailyStats;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Inbound Port for querying aggregated daily statistics for a tenant. */
public interface GetTenantDailyStatsQuery {
  List<TenantDailyStats> getDailyStatsForTenant(UUID tenantId, LocalDate from, LocalDate to);
}
