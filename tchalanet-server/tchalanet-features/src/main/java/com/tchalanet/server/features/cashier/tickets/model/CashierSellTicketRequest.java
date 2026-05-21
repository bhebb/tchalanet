package com.tchalanet.server.features.cashier.tickets.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record CashierSellTicketRequest(
    @NotNull UUID terminalId,
    @NotNull UUID drawId,
    UUID drawChannelId,
    @NotBlank String currency,
    @NotNull @Size(min = 1) @Valid List<CashierTicketLineRequest> lines
) {
}
