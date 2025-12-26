package com.tchalanet.server.features.stats.aggregates.application;

import java.time.LocalDate;
import com.tchalanet.server.common.types.id.TenantId;

public record RecomputeDailyStatsCommand(LocalDate from, LocalDate to, TenantId tenantId) {
}
