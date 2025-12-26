package com.tchalanet.server.core.ledger.application.query.model;
import com.tchalanet.server.common.types.id.TenantId;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.core.ledger.domain.model.LedgerEntry;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record GetLedgerTransactionsQuery(TenantId tenantId, Instant from, Instant to, int limit, int offset) implements Query<List<LedgerEntry>> {
}
