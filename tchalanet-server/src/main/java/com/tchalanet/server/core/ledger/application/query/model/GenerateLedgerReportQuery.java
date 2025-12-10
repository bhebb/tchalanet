package com.tchalanet.server.core.ledger.application.query.model;

import java.util.UUID;
import java.time.LocalDate;

public record GenerateLedgerReportQuery(
    UUID tenantId,
    LocalDate from,
    LocalDate to,
    UUID outletId,
    UUID agentId
) {}

