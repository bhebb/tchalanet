package com.tchalanet.server.core.ledger.application.command.model;

import java.util.UUID;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Start-of-day deposit from agent to physical cashbox. */
public record DepositCashCommand(
    UUID tenantId,
    UUID outletId,
    UUID agentId,
    BigDecimal amount,
    LocalDateTime occurredAt
) {}

