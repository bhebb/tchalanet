package com.tchalanet.server.core.sellerterminal.api.model;

import java.math.BigDecimal;

public record SellerTerminalCommissionStatsView(
    long totalCount,
    long countAtDefaultRate,
    long countWithCustomRate,
    BigDecimal minRate,
    BigDecimal maxRate,
    BigDecimal avgRate
) {
    public static SellerTerminalCommissionStatsView empty() {
        return new SellerTerminalCommissionStatsView(0L, 0L, 0L, null, null, null);
    }
}
