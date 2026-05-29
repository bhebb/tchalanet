package com.tchalanet.server.features.cashier.tickets.model;

import jakarta.validation.constraints.NotBlank;

public record CashierVerifyTicketRequest(
    @NotBlank String scannedValue
) {}
