package com.tchalanet.server.core.sales.api.model.print;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;

public record TicketPrintSellerContext(
    OutletId outletId,
    String outletCode,
    String outletName,

    TerminalId terminalId,
    String terminalCode,
    String terminalLabel,

    SalesSessionId salesSessionId,
    String sessionCode,

    UserId sellerUserId,
    String sellerDisplayName
) {
}
