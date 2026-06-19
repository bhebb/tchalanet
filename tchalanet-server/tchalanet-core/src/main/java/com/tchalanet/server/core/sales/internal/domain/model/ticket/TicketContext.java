package com.tchalanet.server.core.sales.internal.domain.model.ticket;

import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.SellerTerminalId;

import java.math.BigDecimal;
import java.util.Objects;

public record TicketContext(
    DrawId drawId,
    DrawChannelId drawChannelId,
    SellerTerminalId sellerTerminalId,
    BigDecimal sellerCommissionRateSnapshot,
    BigDecimal sellerCommissionAmountSnapshot
) {
    public TicketContext {
        Objects.requireNonNull(drawId, "drawId is required");
        Objects.requireNonNull(drawChannelId, "drawChannelId is required");
    }
}
