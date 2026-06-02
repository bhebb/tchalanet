package com.tchalanet.server.features.ops.draw;

import java.util.List;

/**
 * Generic result of a per-tenant ops sweep (draw generate / open-today / close-due).
 *
 * <p>The op runs once per target tenant, each in that tenant's RLS context; a single tenant's
 * failure is captured per-tenant ({@code ok=false} + {@code error}) without aborting the rest —
 * matching the resilient, partial-failure behaviour of the scheduled jobs.
 *
 * @param <R> the per-tenant command result type
 */
public record TenantBatchResponse<R>(
    int tenantsRequested,
    int tenantsSucceeded,
    int tenantsFailed,
    List<Outcome<R>> tenants) {

  public record Outcome<R>(String tenantId, boolean ok, R result, String error) {}
}
