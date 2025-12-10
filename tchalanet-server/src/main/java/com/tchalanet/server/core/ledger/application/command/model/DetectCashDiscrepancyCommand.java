package com.tchalanet.server.core.ledger.application.command.model;

import java.util.UUID;
import java.time.LocalDate;

/** Runs a check and raises an internal alert/event if diff > threshold. */
public record DetectCashDiscrepancyCommand(
    UUID tenantId,
    LocalDate date,
    java.math.BigDecimal threshold
) {}

