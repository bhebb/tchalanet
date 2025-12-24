package com.tchalanet.server.core.ledger.application.command.model;

import com.tchalanet.server.common.bus.Command;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record WithdrawCashCommand(UUID tenantId, UUID refId, BigDecimal amount, Instant occurredAt) implements Command<Void> {
}
