package com.tchalanet.server.core.sales.internal.domain.event;

import com.tchalanet.server.common.types.enums.BetType;
import org.springframework.lang.Nullable;

public record TicketPlacedLineEvent(
    BetType betType,
    String selectionKeyRaw,
    long stakeCents,
    long potentialPayoutCents,
    @Nullable Short betOption
) {
}
