package com.tchalanet.server.features.ops.draw;

import com.tchalanet.server.core.draw.api.command.GenerateDrawsForRangeResult;
import java.util.List;

/**
 * Result of a (possibly multi-tenant) draw-generation ops call.
 *
 * <p>{@code totals} sums the per-tenant counters; {@code tenants} carries the per-tenant
 * outcome (including a per-tenant error message when one tenant fails without aborting the
 * others — matching the resilient, partial-failure behaviour of the scheduled job).
 */
public record GenerateDrawsBatchResponse(
    int tenantsRequested,
    int tenantsSucceeded,
    int tenantsFailed,
    GenerateDrawsForRangeResult totals,
    List<TenantOutcome> tenants) {

  public record TenantOutcome(
      String tenantId,
      boolean ok,
      GenerateDrawsForRangeResult result,
      String error) {}
}
