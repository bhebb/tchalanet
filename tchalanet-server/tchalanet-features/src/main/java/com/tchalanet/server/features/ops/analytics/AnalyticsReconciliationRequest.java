package com.tchalanet.server.features.ops.analytics;

import com.tchalanet.server.core.analytics.api.model.AnalyticsReconciliationMode;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

/** Platform-ops request for a bounded tenant analytics reconciliation. */
public record AnalyticsReconciliationRequest(
    @NotNull UUID tenantId,
    @NotNull LocalDate from,
    @NotNull LocalDate to,
    @NotNull AnalyticsReconciliationMode mode,
    String repairReason) {}
