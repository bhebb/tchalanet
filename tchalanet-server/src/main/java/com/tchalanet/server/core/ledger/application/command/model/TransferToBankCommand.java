package com.tchalanet.server.core.ledger.application.command.model;

import java.util.UUID;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Transfer from ledger to external bank/MonCash. */
public record TransferToBankCommand(
    UUID tenantId,
    UUID outletId,
    BigDecimal amount,
    String bankReference,
    LocalDateTime occurredAt
) {}

