package com.tchalanet.server.core.sales.api.model.view;

import java.util.List;

public record TenantDailySalesStatsView(
    long ticketCount,
    long salesTotalCents,
    long activeSellerTerminals,
    String currency,
    List<GameSalesStatLine> gameBreakdown) {

    public static TenantDailySalesStatsView empty(String currency) {
        return new TenantDailySalesStatsView(0L, 0L, 0L, currency, List.of());
    }
}
