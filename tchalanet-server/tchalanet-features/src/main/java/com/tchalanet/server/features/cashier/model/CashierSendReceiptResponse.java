package com.tchalanet.server.features.cashier.model;

import com.tchalanet.server.platform.communication.api.model.value.CommunicationChannel;
import com.tchalanet.server.common.types.id.TicketId;

public record CashierSendReceiptResponse(
    TicketId ticketId,
    CommunicationChannel channel,
    boolean accepted
) {}
