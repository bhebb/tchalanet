package com.tchalanet.server.core.sales.internal.domain.event;

import com.tchalanet.server.catalog.game.api.model.BetType;

public record TicketPlacedLineEvent(
    BetType betType,
    String selectionKeyRaw,
    long stakeCents,
    long potentialPayoutCents,
    Short betOption
) {
}
