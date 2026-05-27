package com.tchalanet.server.features.tenantadmin.readiness.model;

import java.util.List;

/**
 * Short readiness projection — consumed by the tenant admin dashboard.
 *
 * MUST NOT contain dashboard KPIs (salesToday, ticketCountToday, activeSessions,
 * openDraws, unread). MUST NOT contain section-level data — only the rolled-up
 * status, the missing count, and a bounded top-issues list.
 */
public record TenantReadinessSummary(
    TenantReadinessStatus status,
    int missingCount,
    List<TenantReadinessIssue> topIssues) {

  public static TenantReadinessSummary unknown() {
    return new TenantReadinessSummary(TenantReadinessStatus.UNKNOWN, 0, List.of());
  }
}
