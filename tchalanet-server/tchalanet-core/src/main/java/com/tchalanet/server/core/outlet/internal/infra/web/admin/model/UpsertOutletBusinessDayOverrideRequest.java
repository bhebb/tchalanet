package com.tchalanet.server.core.outlet.internal.infra.web.admin.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Upsert an OUTLET-LEVEL business-day override (one outlet). The outlet comes
 * from the path; the tenant from the request context. Idempotent on
 * (tenant, outlet, date).
 *
 * <p>{@code open = false} closes that outlet on that date; {@code true} forces it
 * open against a recurring/tenant closure.
 */
public record UpsertOutletBusinessDayOverrideRequest(
    @NotNull LocalDate businessDate,
    boolean open,
    @Size(max = 96) String reasonCode,
    @Size(max = 255) String label) {}
