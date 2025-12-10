package com.tchalanet.server.core.ledger.application.query.model;

import java.util.UUID;

public record GetLedgerBalanceQuery(
    UUID tenantId
) {}

