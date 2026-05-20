package com.tchalanet.server.core.session.internal.domain.model;

public record SalesSessionCashSummary(
    Long openingFloatCents,
    Long salesCashInCents,
    Long payoutCashOutCents,
    Long expectedClosingAmountCents,
    Long declaredClosingAmountCents,
    Long varianceCents) {}
