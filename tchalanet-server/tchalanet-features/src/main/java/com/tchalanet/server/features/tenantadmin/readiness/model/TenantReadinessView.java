package com.tchalanet.server.features.tenantadmin.readiness.model;

import java.util.List;

/**
 * Detailed readiness projection — consumed by the tenant overview endpoint and
 * by the tenant provisioning result. Includes sections with per-section status,
 * issues and frontend routes.
 *
 * MUST NOT contain dashboard KPIs (salesToday, ticketCountToday, activeSessions,
 * openDraws, unread).
 */
public record TenantReadinessView(
    TenantReadinessStatus status,
    int missingCount,
    List<TenantReadinessSection> sections) {

  public static TenantReadinessView unknown() {
    return new TenantReadinessView(TenantReadinessStatus.UNKNOWN, 0, List.of());
  }
}
