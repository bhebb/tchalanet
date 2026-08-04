package com.tchalanet.server.features.ops.analytics;

import com.tchalanet.server.core.analytics.api.model.AnalyticsTrustScopeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

/** Platform-ops request for a tenant/date-scoped analytics visibility override. */
public record AnalyticsVisibilityOverrideRequest(
    @NotNull AnalyticsTrustScopeType scopeType,
    UUID tenantId,
    UUID sellerTerminalId,
    UUID drawId,
    @NotNull LocalDate from,
    @NotNull LocalDate to,
    boolean visible,
    @NotBlank @Size(min = 10, max = 500) String reason) {}
