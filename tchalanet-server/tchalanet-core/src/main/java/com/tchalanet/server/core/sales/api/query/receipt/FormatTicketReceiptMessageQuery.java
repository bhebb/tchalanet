package com.tchalanet.server.core.sales.api.query.receipt;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptMessageContent;
import jakarta.validation.constraints.NotNull;
import java.util.Locale;

public record FormatTicketReceiptMessageQuery(
    @NotNull TicketId ticketId,
    Locale locale
) implements Query<TicketReceiptMessageContent> {
}
