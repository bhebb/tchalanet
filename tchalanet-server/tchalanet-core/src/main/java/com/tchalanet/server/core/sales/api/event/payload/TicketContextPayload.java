package com.tchalanet.server.core.sales.api.event.payload;

import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;

public record TicketContextPayload(
    OutletId outletId,
    TerminalId terminalId,
    UserId sellerUserId,
    SalesSessionId salesSessionId,
    DrawId drawId,
    DrawChannelId drawChannelId
) {
}
