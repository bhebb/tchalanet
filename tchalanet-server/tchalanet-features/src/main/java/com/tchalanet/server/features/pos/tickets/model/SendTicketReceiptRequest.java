package com.tchalanet.server.features.pos.tickets.model;

import com.tchalanet.server.platform.communication.api.model.value.CommunicationChannel;
import jakarta.validation.constraints.NotNull;
import java.util.Locale;
import com.tchalanet.server.common.types.id.SellerTerminalId;

public record SendTicketReceiptRequest(
    @NotNull SellerTerminalId sellerTerminalId,
    @NotNull CommunicationChannel channel,
    String to,
    String channelKey,
    Locale locale
) {
}
