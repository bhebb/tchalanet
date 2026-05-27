package com.tchalanet.server.core.analytics.api.model;

import com.tchalanet.server.common.types.id.OutletId;
import java.math.BigDecimal;

/**
 * One row in an outlet performance report.
 */
public record OutletReportLine(
    OutletId   outletId,
    String     outletCode,
    String     outletName,
    String     gameCode,
    long       ticketsSold,
    BigDecimal totalSales,
    BigDecimal totalPayout,
    BigDecimal netRevenue
) {}
