package com.tchalanet.server.core.payout.application.command.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.tchalanet.server.common.bus.Command;

public record RegisterPayoutCommand(
    UUID tenantId,
    UUID ticketId,
    BigDecimal amount,
    UUID cashierId,
    UUID outletId,
    Instant requestedAt) implements Command<RegisterPayoutResult> {
}
