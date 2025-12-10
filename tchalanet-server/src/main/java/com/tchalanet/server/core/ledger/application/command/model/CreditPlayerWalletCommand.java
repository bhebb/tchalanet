package com.tchalanet.server.core.ledger.application.command.model;

import java.util.UUID;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Auto-credit player wallet for small winnings. */
public record CreditPlayerWalletCommand(
    UUID tenantId,
    UUID playerId,
    BigDecimal amount,
    LocalDateTime occurredAt,
    String reason
) {}

