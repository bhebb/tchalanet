package com.tchalanet.server.core.sales.api.model.receipt;

import java.util.List;

public record TicketReceiptSectionContent(
    String title,
    List<TicketReceiptTextLine> lines
) {
    public TicketReceiptSectionContent {
        lines = lines == null ? List.of() : List.copyOf(lines);
    }
}
