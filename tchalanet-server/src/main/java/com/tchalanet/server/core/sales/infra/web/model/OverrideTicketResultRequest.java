package com.tchalanet.server.core.sales.infra.web.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import jakarta.validation.constraints.NotNull;

public record OverrideTicketResultRequest(
    @NotNull BigDecimal totalPayout,
    @NotNull TicketStatus status,
    String reason,
    UUID performedBy,
    Instant performedAt) {}

