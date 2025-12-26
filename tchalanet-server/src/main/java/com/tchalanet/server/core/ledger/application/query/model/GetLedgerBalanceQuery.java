package com.tchalanet.server.core.ledger.application.query.model;
import com.tchalanet.server.common.types.id.TenantId;

import java.math.BigDecimal;
import java.util.UUID;

import com.tchalanet.server.common.bus.Query;

public record GetLedgerBalanceQuery(TenantId tenantId) implements Query<BigDecimal> {
}
