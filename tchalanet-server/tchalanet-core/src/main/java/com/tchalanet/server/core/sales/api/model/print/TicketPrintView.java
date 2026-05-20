package com.tchalanet.server.core.sales.api.model.print;

import java.util.List;

public record TicketPrintView(
    TicketPrintIdentity identity,
    TicketPrintLifecycle lifecycle,
    TicketPrintState printState,
    TicketPrintDraw draw,
    TicketPrintSellerContext context,
    TicketPrintBranding branding,
    List<TicketPrintLine> lines,
    TicketPrintMoney money,
    TicketPrintQrPayload qr,
    TicketPrintMetadata metadata
) {
    public TicketPrintView {
        lines = List.copyOf(lines);
    }
}
