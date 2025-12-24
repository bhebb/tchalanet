package com.tchalanet.server.core.ledger.application.port.out;

import com.tchalanet.server.core.ledger.domain.model.LedgerEntry;
import com.tchalanet.server.core.ledger.domain.model.LedgerRefType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface LedgerReaderPort {
    BigDecimal getBalance(UUID tenantId);
    List<LedgerEntry> findByTenant(UUID tenantId, Instant from, Instant to, int limit, int offset);
    List<LedgerEntry> findByRef(UUID tenantId, LedgerRefType refType, UUID refId);
    boolean existsByRef(UUID tenantId, LedgerRefType refType, UUID refId);
}
