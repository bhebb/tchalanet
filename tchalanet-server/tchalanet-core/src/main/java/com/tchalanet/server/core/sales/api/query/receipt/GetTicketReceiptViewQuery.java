package com.tchalanet.server.core.sales.api.query.receipt;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.sales.api.model.receipt.TicketReceiptView;
import jakarta.validation.constraints.NotNull;

public record GetTicketReceiptViewQuery(
    @NotNull TicketId ticketId
) implements Query<TicketReceiptView> {
}
