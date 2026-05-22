package com.tchalanet.server.features.cashier.tickets.model;

import com.tchalanet.server.platform.communication.api.model.value.CommunicationChannel;
import jakarta.validation.constraints.NotNull;
import java.util.Locale;
import java.util.UUID;

public record SendTicketReceiptRequest(
    @NotNull UUID terminalId,
    @NotNull CommunicationChannel channel,
    String to,
    String channelKey,
    Locale locale
) {
}
