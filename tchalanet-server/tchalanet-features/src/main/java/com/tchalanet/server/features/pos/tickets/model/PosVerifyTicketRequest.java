package com.tchalanet.server.features.pos.tickets.model;

import jakarta.validation.constraints.NotBlank;

public record PosVerifyTicketRequest(
    @NotBlank String scannedValue
) {}
