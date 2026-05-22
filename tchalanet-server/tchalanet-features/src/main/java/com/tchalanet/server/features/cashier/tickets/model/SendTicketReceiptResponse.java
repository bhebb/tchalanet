package com.tchalanet.server.features.cashier.tickets.model;

import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.platform.communication.api.model.value.CommunicationChannel;

public record SendTicketReceiptResponse(
    TicketId ticketId,
    CommunicationChannel channel,
    String recipient,
    boolean queued,
    boolean deduplicated
) {
}
