package com.tchalanet.server.core.sales.internal.domain.event;

import com.tchalanet.server.common.types.enums.BetType;

public record TicketPlacedLineEvent(
    BetType betType,
    String selectionKeyRaw,
    long stakeCents,
    long potentialPayoutCents,
    Short betOption
) {
}
