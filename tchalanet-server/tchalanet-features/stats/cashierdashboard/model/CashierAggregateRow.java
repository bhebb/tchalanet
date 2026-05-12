package com.tchalanet.server.features.stats.cashierdashboard.model;

import java.util.UUID;

/** Aggregate pour un caissier sur une période donnée, basé sur stats_daily. */
public record CashierAggregateRow(
    UUID cashierId,
    long ticketsCount,
    long stakeSumCents,
    long winningsSumCents,
    long netRevenueCents) {}
