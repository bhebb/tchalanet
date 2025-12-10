package com.tchalanet.server.core.ledger.application.command.model;

import java.util.UUID;
import java.time.LocalDateTime;

/** Reverse sale/payout/transfer with mandatory justification and audit trail. */
public record ReverseTransactionCommand(
    UUID tenantId,
    UUID transactionId,
    UUID requestedBy,
    String justification,
    LocalDateTime requestedAt
) {}

