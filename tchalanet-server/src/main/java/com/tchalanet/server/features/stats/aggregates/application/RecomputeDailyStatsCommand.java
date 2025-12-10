package com.tchalanet.server.features.stats.aggregates.application;

import java.time.LocalDate;
import java.util.UUID;

public record RecomputeDailyStatsCommand(LocalDate from, LocalDate to, UUID tenantId) {
}

