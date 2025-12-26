package com.tchalanet.server.core.ledger.application.port.in;

import com.tchalanet.server.common.types.id.PayoutId;
import com.tchalanet.server.common.types.id.TenantId;
import java.math.BigDecimal;
import java.time.Instant;

public interface RecordLedgerFromPayoutPort {
  void recordPayout(TenantId tenantId, PayoutId payoutId, BigDecimal amount, Instant occurredAt);
}
