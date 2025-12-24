package com.tchalanet.server.core.ledger.application.command.model;

import com.tchalanet.server.common.bus.Command;

import java.time.Instant;
import java.util.UUID;

public record ReconcileDailyLedgerCommand(UUID tenantId, Instant dayStart, Instant dayEnd) implements Command<Void> {
}
