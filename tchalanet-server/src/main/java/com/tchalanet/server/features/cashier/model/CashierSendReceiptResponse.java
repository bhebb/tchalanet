package com.tchalanet.server.features.cashier.model;

import com.tchalanet.server.common.communication.api.CommunicationChannel;
import com.tchalanet.server.common.types.id.TicketId;

public record CashierSendReceiptResponse(
    TicketId ticketId,
    CommunicationChannel channel,
    boolean accepted
) {}
