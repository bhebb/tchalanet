package com.tchalanet.server.core.ledger.application.port.out;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.ledger.domain.model.LedgerEntry;
import com.tchalanet.server.core.ledger.domain.model.LedgerRefType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface LedgerReaderPort {
  BigDecimal getBalance(TenantId tenantId);

  List<LedgerEntry> findByTenant(
      TenantId tenantId, Instant from, Instant to, int limit, int offset);

  List<LedgerEntry> findByRef(TenantId tenantId, LedgerRefType refType, UUID refId);

  boolean existsByRef(TenantId tenantId, LedgerRefType refType, UUID refId);
}
