package com.tchalanet.server.core.ledger.application.command.model;

import java.util.UUID;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** End-of-day withdrawal / cash-out. */
public record WithdrawCashCommand(
    UUID tenantId,
    UUID outletId,
    UUID agentId,
    BigDecimal amount,
    LocalDateTime occurredAt
) {}

