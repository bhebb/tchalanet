package com.tchalanet.server.core.ledger.application.query.model;

import java.util.UUID;
import java.time.LocalDate;

public record GetLedgerTransactionsQuery(
    UUID tenantId,
    UUID outletId,
    UUID agentId,
    LocalDate from,
    LocalDate to,
    int page,
    int size
) {}

