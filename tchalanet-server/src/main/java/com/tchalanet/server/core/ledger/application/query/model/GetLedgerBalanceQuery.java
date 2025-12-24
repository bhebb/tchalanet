package com.tchalanet.server.core.ledger.application.query.model;

import java.math.BigDecimal;
import java.util.UUID;

import com.tchalanet.server.common.bus.Query;

public record GetLedgerBalanceQuery(UUID tenantId) implements Query<BigDecimal> {
}
