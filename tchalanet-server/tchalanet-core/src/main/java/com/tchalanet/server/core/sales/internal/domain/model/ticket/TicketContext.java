package com.tchalanet.server.core.sales.internal.domain.model.ticket;

import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.SellerId;
import com.tchalanet.server.common.types.id.SellerOutletAssignmentId;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;

import java.math.BigDecimal;
import java.util.Objects;

public record TicketContext(
    OutletId outletId,
    TerminalId terminalId,
    UserId sellerUserId,
    SalesSessionId salesSessionId,
    DrawId drawId,
    DrawChannelId drawChannelId,
    SellerId sellerId,
    SellerOutletAssignmentId sellerAssignmentId,
    // SellerTerminal path — null on the legacy POS path
    SellerTerminalId sellerTerminalId,
    BigDecimal sellerCommissionRateSnapshot,
    BigDecimal sellerCommissionAmountSnapshot
) {
    public TicketContext {
        Objects.requireNonNull(drawId, "drawId is required");
        Objects.requireNonNull(drawChannelId, "drawChannelId is required");
    }
}
