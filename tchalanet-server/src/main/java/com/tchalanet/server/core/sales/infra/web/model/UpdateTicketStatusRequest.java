package com.tchalanet.server.core.sales.infra.web.model;
import com.tchalanet.server.common.types.id.UserId;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record UpdateTicketStatusRequest(
    @NotNull UUID userId, boolean isAdmin // Flag to indicate admin privileges
    ) {}
