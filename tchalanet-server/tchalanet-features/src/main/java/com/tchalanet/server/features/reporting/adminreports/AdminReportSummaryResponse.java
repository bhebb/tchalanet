package com.tchalanet.server.features.reporting.adminreports;

import java.math.BigDecimal;

public record AdminReportSummaryResponse(
    long ticketsSold,
    BigDecimal grossSales,
    BigDecimal winningsCalculated,
    BigDecimal payoutsPaid,
    BigDecimal sellerCommission,
    BigDecimal buyerCharges,
    BigDecimal sellerCharges,
    BigDecimal tenantCharges,
    BigDecimal waivedCharges,
    long promotionLines,
    long promotionPricedLines,
    BigDecimal promotionPayoutBase,
    BigDecimal netRevenueEstimated,
    BigDecimal netRevenuePaidBasis
) {}
