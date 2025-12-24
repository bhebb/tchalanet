package com.tchalanet.server.core.ledger.application.port.in;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public interface RecordLedgerFromPayoutPort {
    void recordPayout(UUID tenantId, UUID payoutId, BigDecimal amount, Instant occurredAt);
}
