package com.tchalanet.server.core.sales.internal.domain.model.ticket;

import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.SellerId;
import com.tchalanet.server.common.types.id.SellerOutletAssignmentId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;

import java.util.Objects;

public record TicketContext(
    OutletId outletId,
    TerminalId terminalId,
    UserId sellerUserId,
    SalesSessionId salesSessionId,
    DrawId drawId,
    DrawChannelId drawChannelId,
    SellerId sellerId,
    SellerOutletAssignmentId sellerAssignmentId
) {
    public TicketContext {
        Objects.requireNonNull(outletId, "outletId is required");
        Objects.requireNonNull(terminalId, "terminalId is required");
        Objects.requireNonNull(sellerUserId, "sellerUserId is required");
        Objects.requireNonNull(salesSessionId, "salesSessionId is required");
        Objects.requireNonNull(drawId, "drawId is required");
        Objects.requireNonNull(drawChannelId, "drawChannelId is required");
    }
}
