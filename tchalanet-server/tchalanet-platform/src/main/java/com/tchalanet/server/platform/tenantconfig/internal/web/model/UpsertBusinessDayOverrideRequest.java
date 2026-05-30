package com.tchalanet.server.platform.tenantconfig.internal.web.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Upsert a TENANT-LEVEL business-day override (whole commerce).
 *
 * <p>Outlet-level closures are a separate, core.outlet-owned surface
 * ({@code /admin/outlets/{outletId}/business-days}). Idempotent on (tenant, date).
 *
 * <p>{@code open = false} marks the day closed (the common case); {@code true}
 * forces the day open against a recurring closed-weekday rule.
 */
public record UpsertBusinessDayOverrideRequest(
    @NotNull LocalDate businessDate,
    boolean open,
    @Size(max = 96) String reasonCode,
    @Size(max = 255) String label) {}
