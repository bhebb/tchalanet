package com.tchalanet.server.core.ledger.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.core.ledger.domain.model.LedgerRefType;

import java.time.Instant;
import java.util.UUID;

public record ReverseTransactionCommand(UUID tenantId, LedgerRefType refType, UUID refId, Instant occurredAt) implements Command<Void> {
}
