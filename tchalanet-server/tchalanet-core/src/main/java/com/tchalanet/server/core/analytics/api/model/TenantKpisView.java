package com.tchalanet.server.core.analytics.api.model;

import java.math.BigDecimal;

/**
 * KPI snapshot for a tenant over a date window.
 *
 * <p>Used by both the reporting endpoint and any dashboard consumer.
 * Values are derived from the analytics projection tables.
 */
public record TenantKpisView(
    long       ticketsSold,
    BigDecimal totalSales,
    BigDecimal totalPayout,
    BigDecimal netRevenue,
    long       activeOutlets,
    long       activeCashiers
) {

  public static TenantKpisView empty() {
    return new TenantKpisView(0L, BigDecimal.ZERO, BigDecimal.ZERO,
        BigDecimal.ZERO, 0L, 0L);
  }
}
