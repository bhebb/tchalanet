package com.tchalanet.server.features.stats.cashier_dashboard.application;

import java.util.UUID;

/**
 * Aggregate pour un caissier sur une période donnée,
 * basé sur stats_daily.
 */
public record CashierAggregateRow(
    UUID cashierId,
    UUID outletId,
    long ticketsCount,
    long stakeSumCents,
    long winningsSumCents,
    long netRevenueCents
) {}
