package com.tchalanet.server.core.sales.api.query;

import java.time.LocalDate;
import java.util.List;

public record CashierDashboardOverviewView(
    LocalDate businessDate,
    long ticketCount,
    long salesTotalCents,
    long cancelledCount,
    long pendingApprovalCount,
    List<DrawBreakdown> byDraw
) {
    public record DrawBreakdown(
        String channelCode,
        String channelLabel,
        long ticketCount,
        long salesTotalCents
    ) {}
}
