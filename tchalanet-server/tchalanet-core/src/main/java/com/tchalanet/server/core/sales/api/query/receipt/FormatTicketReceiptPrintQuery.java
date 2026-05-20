package com.tchalanet.server.core.sales.api.query.receipt;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.sales.api.model.print.PrintOutputFormat;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptPrintContent;
import jakarta.validation.constraints.NotNull;
import java.util.Locale;

public record FormatTicketReceiptPrintQuery(
    @NotNull TicketId ticketId,
    @NotNull PrintOutputFormat format,
    Locale locale
) implements Query<TicketReceiptPrintContent> {
}
