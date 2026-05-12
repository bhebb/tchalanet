package com.tchalanet.server.core.sales.api.event;

public record TicketPlacedLineEvent(
    String gameCode,
    String selection,
    String betType,
    Short betOption,
    long stakeAmountCents,
    long oddsSnapshotScaled,
    long potentialPayoutAmountCents
) {}
