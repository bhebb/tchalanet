package com.tchalanet.server.features.stats.aggregates.application;

import com.tchalanet.server.common.types.id.TenantId;
import java.time.LocalDate;

public record RecomputeDailyStatsCommand(LocalDate from, LocalDate to, TenantId tenantId) {}
