package com.tchalanet.server.core.sales.api.model.print;

import com.tchalanet.server.common.types.id.SellerTerminalId;

public record TicketPrintSellerContext(
    SellerTerminalId sellerTerminalId,
    String sellerTerminalCode,
    String sellerTerminalLabel,
    String sellerTerminalDisplayName
) {
}
