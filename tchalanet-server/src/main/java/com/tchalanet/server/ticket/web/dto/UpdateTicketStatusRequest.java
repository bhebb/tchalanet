package com.tchalanet.server.ticket.web.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record UpdateTicketStatusRequest(
    @NotNull UUID userId, boolean isAdmin // Flag to indicate admin privileges
    ) {}
