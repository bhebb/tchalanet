package com.tchalanet.server.features.cashier.tickets.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CashierTicketCancelRequest(
    @NotNull UUID terminalId,
    @Size(max = 500) String reason
) {
}
