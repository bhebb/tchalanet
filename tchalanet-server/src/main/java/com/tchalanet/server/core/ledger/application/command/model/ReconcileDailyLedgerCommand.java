package com.tchalanet.server.core.ledger.application.command.model;

import java.util.UUID;
import java.time.LocalDate;

/** Batch at 02:00: compare physical vs logical cash. */
public record ReconcileDailyLedgerCommand(
    UUID tenantId,
    LocalDate date
) {}

