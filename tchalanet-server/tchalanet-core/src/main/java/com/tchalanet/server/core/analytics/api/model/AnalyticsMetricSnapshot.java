package com.tchalanet.server.core.analytics.api.model;

/**
 * Immutable analytics totals expressed in the tenant currency's minor units.
 *
 * <p>Keeping the reconciliation oracle in minor units makes comparisons independent from display
 * currency formatting and rounding in clients.
 */
public record AnalyticsMetricSnapshot(
    long ticketsSold,
    long ticketsCancelled,
    long grossSalesMinor,
    long stakeTotalMinor,
    long winningsCalculatedMinor,
    long payoutsPaidMinor,
    long sellerCommissionMinor,
    long buyerChargeMinor,
    long sellerChargeMinor,
    long tenantChargeMinor,
    long waivedChargeMinor,
    long promotionLineCount,
    long promotionPricedLineCount) {

  public long netRevenueEstimatedMinor() {
    return grossSalesMinor - winningsCalculatedMinor - sellerCommissionMinor - tenantChargeMinor;
  }

  public long netRevenuePaidBasisMinor() {
    return grossSalesMinor - payoutsPaidMinor - sellerCommissionMinor - tenantChargeMinor;
  }
}
