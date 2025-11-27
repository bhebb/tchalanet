package com.tchalanet.server.stats.domain.ports.out;

import com.tchalanet.server.stats.domain.model.DrawStats;
import com.tchalanet.server.stats.domain.model.TenantDailyStats;

/** Outbound Port for persisting calculated statistics. */
public interface StatsRepositoryPort {
  void upsertDrawStats(DrawStats drawStats);

  void upsertTenantDailyStats(TenantDailyStats tenantDailyStats);
}
