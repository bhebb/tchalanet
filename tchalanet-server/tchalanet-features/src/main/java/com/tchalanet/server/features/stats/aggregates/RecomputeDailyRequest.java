package com.tchalanet.server.features.stats.aggregates;

import com.tchalanet.server.common.types.id.TenantId;
import java.time.LocalDate;

public record RecomputeDailyRequest(LocalDate from, LocalDate to, TenantId tenantId) {}
