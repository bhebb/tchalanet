package com.tchalanet.server.core.payout.application.query.model;

public record PayoutSessionSummary(
    long requestedCount,
    long approvedCount,
    long rejectedCount,
    long paidCount,
    long paidAmountCents,
    long cancelledCount
) {}
