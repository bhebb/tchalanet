package com.tchalanet.server.features.pos.tickets.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record PosTicketCancelRequest(
    @NotNull UUID sellerTerminalId,
    @Size(max = 500) String reason
) {
}
