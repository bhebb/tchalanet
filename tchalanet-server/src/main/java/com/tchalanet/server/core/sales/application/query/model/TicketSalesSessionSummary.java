package com.tchalanet.server.core.sales.application.query.model;

public record TicketSalesSessionSummary(
    long soldCount,
    long soldAmountCents,
    long pendingApprovalCount,
    long voidCount,
    long rejectedCount,
    long wonCount,
    long wonAmountCents,
    long lostCount,
    long settledCount
) {}
